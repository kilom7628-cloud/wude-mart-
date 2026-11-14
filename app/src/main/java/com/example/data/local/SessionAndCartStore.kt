package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CartEntryStorage
import com.example.data.model.CustomerSession
import com.example.data.model.DeliveryProfileStorage
import com.example.data.model.ProductDto
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemePreference
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.wudeDataStore: DataStore<Preferences> by preferencesDataStore(name = "wude_mart_prefs")

class SessionAndCartStore(
  context: Context,
  private val moshi: Moshi,
) {
  private val dataStore = context.applicationContext.wudeDataStore

  private object Keys {
    val ENCRYPTED_TOKEN = stringPreferencesKey("enc_token")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_ROLE = stringPreferencesKey("user_role")
    val USER_ID = longPreferencesKey("user_id")
    val USER_CREATED_AT = stringPreferencesKey("user_created_at")
    val CART_JSON = stringPreferencesKey("cart_json")
    val WISHLIST_MAP_JSON = stringPreferencesKey("wishlist_map_json")
    val DELIVERY_JSON = stringPreferencesKey("delivery_json")
    val CACHED_PRODUCTS_JSON = stringPreferencesKey("cached_products_json")
    val THEME_PREF = stringPreferencesKey("theme_pref")
    val LANGUAGE_PREF = stringPreferencesKey("language_pref")
  }

  private val cartListType =
    Types.newParameterizedType(List::class.java, CartEntryStorage::class.java)
  private val cartAdapter = moshi.adapter<List<CartEntryStorage>>(cartListType)

  private val productListType =
    Types.newParameterizedType(List::class.java, ProductDto::class.java)
  private val productsAdapter = moshi.adapter<List<ProductDto>>(productListType)

  private val longListType =
    Types.newParameterizedType(List::class.java, Long::class.javaObjectType)
  private val wishlistMapType =
    Types.newParameterizedType(Map::class.java, String::class.java, longListType)
  private val wishlistMapAdapter = moshi.adapter<Map<String, List<Long>>>(wishlistMapType)

  private val deliveryAdapter = moshi.adapter(DeliveryProfileStorage::class.java)

  val sessionFlow: Flow<CustomerSession?> =
    dataStore.data.map { prefs ->
      val enc = prefs[Keys.ENCRYPTED_TOKEN].orEmpty()
      val token = SecureTokenCipher.decrypt(enc)
      val email = prefs[Keys.USER_EMAIL].orEmpty()
      if (token.isBlank() || email.isBlank()) {
        null
      } else {
        CustomerSession(
          token = token,
          email = email,
          role = prefs[Keys.USER_ROLE] ?: "customer",
          userId = prefs[Keys.USER_ID],
          createdAt = prefs[Keys.USER_CREATED_AT],
        )
      }
    }

  suspend fun getAccessToken(): String? {
    val prefs = dataStore.data.first()
    val enc = prefs[Keys.ENCRYPTED_TOKEN].orEmpty()
    return SecureTokenCipher.decrypt(enc).takeIf { it.isNotBlank() }
  }

  suspend fun saveSession(session: CustomerSession) {
    val encrypted = SecureTokenCipher.encrypt(session.token)
    dataStore.edit { prefs ->
      prefs[Keys.ENCRYPTED_TOKEN] = encrypted
      prefs[Keys.USER_EMAIL] = session.email
      prefs[Keys.USER_ROLE] = session.role
      if (session.userId != null) {
        prefs[Keys.USER_ID] = session.userId
      } else {
        prefs.remove(Keys.USER_ID)
      }
      if (!session.createdAt.isNullOrBlank()) {
        prefs[Keys.USER_CREATED_AT] = session.createdAt
      }
    }
  }

  suspend fun clearSession() {
    dataStore.edit { prefs ->
      prefs.remove(Keys.ENCRYPTED_TOKEN)
      prefs.remove(Keys.USER_EMAIL)
      prefs.remove(Keys.USER_ROLE)
      prefs.remove(Keys.USER_ID)
      prefs.remove(Keys.USER_CREATED_AT)
    }
  }

  val cartFlow: Flow<List<CartEntryStorage>> =
    dataStore.data.map { prefs ->
      val json = prefs[Keys.CART_JSON].orEmpty()
      if (json.isBlank()) {
        emptyList()
      } else {
        runCatching { cartAdapter.fromJson(json) }.getOrNull().orEmpty()
      }
    }

  suspend fun saveCart(entries: List<CartEntryStorage>) {
    val json = runCatching { cartAdapter.toJson(entries) }.getOrDefault("[]")
    dataStore.edit { prefs -> prefs[Keys.CART_JSON] = json }
  }

  fun wishlistFlow(accountKey: String): Flow<Set<Long>> =
    dataStore.data.map { prefs ->
      val map = parseWishlistMap(prefs[Keys.WISHLIST_MAP_JSON].orEmpty())
      val accountIds = map[accountKey].orEmpty()
      val guestIds = if (accountKey != "guest") map["guest"].orEmpty() else emptyList()
      (accountIds + guestIds).toSet()
    }

  suspend fun toggleWishlist(accountKey: String, productId: Long): Boolean {
    var added = false
    dataStore.edit { prefs ->
      val mutable = parseWishlistMap(prefs[Keys.WISHLIST_MAP_JSON].orEmpty()).toMutableMap()
      val current = mutable[accountKey].orEmpty().toMutableList()
      if (current.contains(productId)) {
        current.removeAll { it == productId }
        added = false
      } else {
        current.add(0, productId)
        added = true
      }
      mutable[accountKey] = current.distinct()
      prefs[Keys.WISHLIST_MAP_JSON] =
        runCatching { wishlistMapAdapter.toJson(mutable) }.getOrDefault("{}")
    }
    return added
  }

  private fun parseWishlistMap(json: String): Map<String, List<Long>> {
    if (json.isBlank()) return emptyMap()
    return runCatching { wishlistMapAdapter.fromJson(json) }.getOrNull().orEmpty()
  }

  val deliveryProfileFlow: Flow<DeliveryProfileStorage> =
    dataStore.data.map { prefs ->
      val json = prefs[Keys.DELIVERY_JSON].orEmpty()
      if (json.isBlank()) {
        DeliveryProfileStorage()
      } else {
        runCatching { deliveryAdapter.fromJson(json) }.getOrNull() ?: DeliveryProfileStorage()
      }
    }

  suspend fun saveDeliveryProfile(profile: DeliveryProfileStorage) {
    val json = runCatching { deliveryAdapter.toJson(profile) }.getOrDefault("")
    dataStore.edit { prefs -> prefs[Keys.DELIVERY_JSON] = json }
  }

  suspend fun saveCachedProducts(products: List<ProductDto>) {
    val json = runCatching { productsAdapter.toJson(products) }.getOrDefault("[]")
    dataStore.edit { prefs -> prefs[Keys.CACHED_PRODUCTS_JSON] = json }
  }

  suspend fun getCachedProducts(): List<ProductDto> {
    val json = dataStore.data.first()[Keys.CACHED_PRODUCTS_JSON].orEmpty()
    if (json.isBlank()) return emptyList()
    return runCatching { productsAdapter.fromJson(json) }.getOrNull().orEmpty()
  }

  val themePreferenceFlow: Flow<ThemePreference> =
    dataStore.data.map { prefs ->
      when (prefs[Keys.THEME_PREF]) {
        ThemePreference.LIGHT.name -> ThemePreference.LIGHT
        ThemePreference.DARK.name -> ThemePreference.DARK
        else -> ThemePreference.SYSTEM
      }
    }

  suspend fun saveThemePreference(pref: ThemePreference) {
    dataStore.edit { it[Keys.THEME_PREF] = pref.name }
  }

  val languagePreferenceFlow: Flow<AppLanguage> =
    dataStore.data.map { prefs ->
      when (prefs[Keys.LANGUAGE_PREF]) {
        AppLanguage.AMHARIC.name -> AppLanguage.AMHARIC
        else -> AppLanguage.ENGLISH
      }
    }

  suspend fun saveLanguagePreference(language: AppLanguage) {
    dataStore.edit { it[Keys.LANGUAGE_PREF] = language.name }
  }
}
