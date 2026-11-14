package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.i18n.LocalWudeStrings
import com.example.ui.theme.EthiopicFontFamily
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerBg
import com.example.ui.viewmodel.AuthFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  formState: AuthFormState,
  onBack: () -> Unit,
  onSetMode: (Boolean) -> Unit,
  onSubmit: (String, String) -> Unit,
  onClearError: () -> Unit,
) {
  val strings = LocalWudeStrings.current
  var email by rememberSaveable { mutableStateOf("") }
  var password by rememberSaveable { mutableStateOf("") }
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (formState.isRegisterMode) strings.createAccount else strings.signIn) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("auth_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          ),
      )
    }
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 520.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_wude_icon),
          contentDescription = strings.appName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(68.dp).clip(RoundedCornerShape(18.dp)),
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = strings.appName,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = strings.guestSubtitle,
          fontFamily = EthiopicFontFamily,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = !formState.isRegisterMode,
            onClick = { onSetMode(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("auth_mode_login"),
          ) {
            Text(strings.signIn)
          }
          SegmentedButton(
            selected = formState.isRegisterMode,
            onClick = { onSetMode(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("auth_mode_register"),
          ) {
            Text(strings.createAccount)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        GlassCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            OutlinedTextField(
              value = email,
              onValueChange = {
                email = it
                onClearError()
              },
              label = { Text(strings.emailAddress) },
              placeholder = { Text("abebe@example.com") },
              leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
                ),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
            )

            OutlinedTextField(
              value = password,
              onValueChange = {
                password = it
                onClearError()
              },
              label = { Text(strings.password) },
              placeholder = { Text("8+ characters") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = { passwordVisible = !passwordVisible },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("toggle_password_visibility"),
                ) {
                  Icon(
                    imageVector =
                      if (passwordVisible) Icons.Filled.VisibilityOff
                      else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  )
                }
              },
              visualTransformation =
                if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction = ImeAction.Done,
                ),
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
            )

            if (formState.errorMessage != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusDangerBg,
              ) {
                Text(
                  text = formState.errorMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = StatusDanger,
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
              }
            }

            Button(
              onClick = { onSubmit(email, password) },
              enabled = !formState.isSubmitting,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(vertical = 14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("auth_submit_button"),
            ) {
              if (formState.isSubmitting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("…")
              } else {
                Text(
                  text =
                    if (formState.isRegisterMode) strings.createWudeAccount
                    else strings.signIn,
                  style = MaterialTheme.typography.titleSmall,
                )
              }
            }
          }
        }
      }
    }
  }
}
