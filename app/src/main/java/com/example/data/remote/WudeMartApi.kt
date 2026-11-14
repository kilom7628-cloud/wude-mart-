package com.example.data.remote

import com.example.data.model.AuthRequest
import com.example.data.model.AuthResponse
import com.example.data.model.CreateOrderRequest
import com.example.data.model.FulfillmentChoiceRequest
import com.example.data.model.OrderDto
import com.example.data.model.PaymentDto
import com.example.data.model.ProductDto
import com.example.data.model.UserProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface WudeMartApi {

  @GET("api/products")
  suspend fun getProducts(): List<ProductDto>

  @GET("api/products/{id}")
  suspend fun getProductById(@Path("id") productId: Long): ProductDto

  @POST("api/auth/register")
  suspend fun register(@Body request: AuthRequest): Response<AuthResponse>

  @POST("api/auth/login")
  suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

  @GET("api/auth/me")
  suspend fun getMe(): Response<UserProfileDto>

  @GET("api/orders")
  suspend fun getOrders(): Response<List<OrderDto>>

  @GET("api/orders/{id}")
  suspend fun getOrderById(@Path("id") orderId: Long): Response<OrderDto>

  @POST("api/orders")
  suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderDto>

  @Multipart
  @POST("api/orders/{id}/payment")
  suspend fun uploadPaymentProof(
    @Path("id") orderId: Long,
    @Part proof: MultipartBody.Part,
    @Part("payment_method") paymentMethod: RequestBody? = null,
  ): Response<PaymentDto>

  @GET("api/payment-methods")
  suspend fun getPaymentMethodsRaw(): Response<ResponseBody>

  @GET("api/orders/{id}/messages")
  suspend fun getOrderMessagesRaw(@Path("id") orderId: Long): Response<ResponseBody>

  @GET("api/store-config")
  suspend fun getStoreConfigRaw(): Response<ResponseBody>

  @POST("api/delivery/quote")
  suspend fun getDeliveryQuote(@Body request: com.example.data.model.DeliveryQuoteRequest): Response<ResponseBody>

  @POST("api/delivery/pricing")
  suspend fun getDeliveryPricingQuote(@Body request: com.example.data.model.DeliveryQuoteRequest): Response<ResponseBody>

  @POST("api/orders/{id}/fulfillment")
  suspend fun submitFulfillmentChoice(
    @Path("id") orderId: Long,
    @Body request: FulfillmentChoiceRequest,
  ): Response<OrderDto>

  @GET("api/orders/{id}/tracking")
  suspend fun getOrderTrackingRaw(@Path("id") orderId: Long): Response<ResponseBody>
}
