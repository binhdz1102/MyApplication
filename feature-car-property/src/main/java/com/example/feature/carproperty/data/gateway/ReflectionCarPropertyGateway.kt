package com.example.feature.carproperty.data.gateway

import android.content.Context
import android.content.pm.PackageManager
import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyStatus
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Singleton
class ReflectionCarPropertyGateway @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : CarPropertyGateway {

    @Volatile
    private var carServiceHolder: CarServiceHolder? = null

    private data class CarServiceHolder(
        val car: Any,
        val propertyManager: Any,
    )

    private data class ResolvedProperty(
        val propertyId: Int,
        val areaId: Int,
    )

    private val carPropertyValueClass: Class<*> by lazy {
        Class.forName("android.car.hardware.CarPropertyValue")
    }

    private val statusAvailable: Int by lazy {
        carPropertyValueClass.getField("STATUS_AVAILABLE").getInt(null)
    }

    private val statusUnavailable: Int by lazy {
        carPropertyValueClass.getField("STATUS_UNAVAILABLE").getInt(null)
    }

    override suspend fun <T : Any> readOnce(template: CarPropertyTemplate<T>): CarPropertyReading<T> {
        return withContext(Dispatchers.IO) {
            val resolved = resolveProperty(template)
            val propertyManager = requirePropertyManager()
            val method = propertyManager.javaClass.methods.firstOrNull {
                it.name == "getProperty" && it.parameterCount == 2
            } ?: error("CarPropertyManager.getProperty(int, int) not found")
            val rawValue = method.invoke(propertyManager, resolved.propertyId, resolved.areaId)
                ?: error("Car property read returned null")
            mapReading(template, rawValue, resolved)
        }
    }

    override fun <T : Any> observe(template: CarPropertyTemplate<T>): Flow<CarPropertyReading<T>> = callbackFlow {
        val resolved = resolveProperty(template)
        val propertyManager = requirePropertyManager()
        val callbackInterface = Class.forName(
            "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
        )

        val callback = Proxy.newProxyInstance(
            callbackInterface.classLoader,
            arrayOf(callbackInterface),
        ) { proxy, method, args ->
            when (method.name) {
                "onChangeEvent" -> {
                    val rawValue = args?.firstOrNull() ?: return@newProxyInstance null
                    trySend(mapReading(template, rawValue, resolved))
                    null
                }

                "onErrorEvent" -> {
                    val propertyId = (args?.getOrNull(0) as? Int) ?: resolved.propertyId
                    val areaId = (args?.getOrNull(1) as? Int) ?: resolved.areaId
                    val errorCode = args?.getOrNull(2) as? Int
                    trySend(
                        CarPropertyReading(
                            templateName = template.displayName,
                            value = null,
                            status = CarPropertyStatus.ERROR,
                            timestampNanos = 0L,
                            propertyId = propertyId,
                            areaId = areaId,
                            errorCode = errorCode,
                        ),
                    )
                    null
                }

                "toString" -> "CarPropertyEventCallbackProxy(${template.displayName})"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }

        val subscribeMethod = propertyManager.javaClass.methods.firstOrNull {
            it.name == "subscribePropertyEvents" && it.parameterCount == 3
        } ?: error("CarPropertyManager.subscribePropertyEvents(int, int, callback) not found")

        val subscribed = subscribeMethod.invoke(
            propertyManager,
            resolved.propertyId,
            resolved.areaId,
            callback,
        ) as? Boolean ?: true

        if (!subscribed) {
            close(IllegalStateException("subscribePropertyEvents returned false"))
        }

        awaitClose {
            val unsubscribeMethod = propertyManager.javaClass.methods.firstOrNull {
                it.name == "unsubscribePropertyEvents" && it.parameterCount == 2
            }
            if (unsubscribeMethod != null) {
                unsubscribeMethod.invoke(propertyManager, resolved.propertyId, callback)
            } else {
                propertyManager.javaClass.methods.firstOrNull {
                    it.name == "unregisterCallback" && it.parameterCount == 2
                }?.invoke(propertyManager, callback, resolved.propertyId)
            }
        }
    }

    override suspend fun <T : Any> set(template: CarPropertyTemplate<T>, value: T) {
        withContext(Dispatchers.IO) {
            val resolved = resolveProperty(template)
            val propertyManager = requirePropertyManager()
            val method = propertyManager.javaClass.methods.firstOrNull {
                it.name == "setProperty" && it.parameterCount == 4
            } ?: error("CarPropertyManager.setProperty(Class, int, int, Object) not found")
            method.invoke(
                propertyManager,
                template.valueClass,
                resolved.propertyId,
                resolved.areaId,
                value,
            )
        }
    }

    private fun requirePropertyManager(): Any {
        carServiceHolder?.let { return it.propertyManager }
        return synchronized(this) {
            carServiceHolder?.let { return@synchronized it.propertyManager }
            val carClass = Class.forName("android.car.Car")
            val createCarMethod = carClass.methods.firstOrNull {
                it.name == "createCar" &&
                    it.parameterCount == 2 &&
                    it.parameterTypes[0] == Context::class.java
            } ?: error("android.car.Car.createCar(Context, Handler) not found")

            val car = createCarMethod.invoke(null, appContext, null)
                ?: error("android.car.Car.createCar returned null")
            val propertyServiceName = carClass.getField("PROPERTY_SERVICE").get(null) as String
            val getCarManagerMethod = carClass.getMethod("getCarManager", String::class.java)
            val propertyManager = getCarManagerMethod.invoke(car, propertyServiceName)
                ?: error("Car property manager is unavailable")

            CarServiceHolder(car = car, propertyManager = propertyManager)
                .also { carServiceHolder = it }
                .propertyManager
        }
    }

    private fun <T : Any> resolveProperty(template: CarPropertyTemplate<T>): ResolvedProperty {
        ensureAutomotiveRuntime()
        return ResolvedProperty(
            propertyId = readStaticInt(template.propertyClassName, template.propertyFieldName),
            areaId = readStaticInt(template.areaClassName, template.areaFieldName),
        )
    }

    private fun ensureAutomotiveRuntime() {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            error("This device does not expose PackageManager.FEATURE_AUTOMOTIVE.")
        }
    }

    private fun readStaticInt(className: String, fieldName: String): Int {
        return Class.forName(className).getField(fieldName).getInt(null)
    }

    private fun <T : Any> mapReading(
        template: CarPropertyTemplate<T>,
        rawValue: Any,
        fallback: ResolvedProperty,
    ): CarPropertyReading<T> {
        val propertyId = invokeNoArg<Int>(rawValue, "getPropertyId")
        val areaId = invokeNoArg<Int>(rawValue, "getAreaId")
        val timestampNanos = invokeNoArg<Long>(rawValue, "getTimestamp")
        val statusCode = invokeNoArg<Int>(rawValue, "getStatus")
        val value = invokeNoArg<Any?>(rawValue, "getValue")

        return CarPropertyReading(
            templateName = template.displayName,
            value = castValue(template, value),
            status = mapStatus(statusCode),
            timestampNanos = timestampNanos,
            propertyId = propertyId.takeIf { it != 0 } ?: fallback.propertyId,
            areaId = areaId.takeIf { it != 0 } ?: fallback.areaId,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> castValue(template: CarPropertyTemplate<T>, rawValue: Any?): T? {
        if (rawValue == null) {
            return null
        }
        if (!template.valueClass.isInstance(rawValue)) {
            error(
                "Unexpected value type ${rawValue.javaClass.name} for ${template.displayName}. " +
                    "Expected ${template.valueClass.name}.",
            )
        }
        return rawValue as T
    }

    private fun mapStatus(statusCode: Int): CarPropertyStatus {
        return when (statusCode) {
            statusAvailable -> CarPropertyStatus.AVAILABLE
            statusUnavailable -> CarPropertyStatus.UNAVAILABLE
            else -> CarPropertyStatus.ERROR
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> invokeNoArg(instance: Any, methodName: String): T {
        val method = instance.javaClass.methods.firstOrNull {
            it.name == methodName && it.parameterCount == 0
        } ?: error("Method $methodName() not found on ${instance.javaClass.name}")
        return method.invoke(instance) as T
    }
}
