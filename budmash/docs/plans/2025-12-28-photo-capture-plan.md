# Photo Capture Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace URL-based WebView scraping with photo capture (camera/gallery) for menu extraction.

**Architecture:** User captures photo of menu via camera or gallery picker. Image bytes are passed to VisionMenuExtractor which sends to Gemini Flash for strain extraction. Existing terpene resolution and results display remain unchanged.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, ActivityResultContracts (camera/gallery), existing VisionMenuExtractor + OpenRouter API

---

### Task 1: Create ImageCapture expect/actual Interface

**Files:**
- Create: `shared/src/commonMain/kotlin/com/budmash/capture/ImageCapture.kt`
- Create: `shared/src/androidMain/kotlin/com/budmash/capture/ImageCapture.android.kt`
- Create: `shared/src/iosMain/kotlin/com/budmash/capture/ImageCapture.ios.kt`

**Step 1: Create common expect class**

```kotlin
// shared/src/commonMain/kotlin/com/budmash/capture/ImageCapture.kt
package com.budmash.capture

sealed class ImageCaptureResult {
    data class Success(val base64Image: String, val width: Int, val height: Int) : ImageCaptureResult()
    data class Error(val message: String) : ImageCaptureResult()
    data object Cancelled : ImageCaptureResult()
}

expect class ImageCapture {
    fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit)
    fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit)
}

object ImageCaptureContext {
    var launcher: ImageCaptureLauncher? = null
}

expect interface ImageCaptureLauncher {
    fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
```

**Step 2: Create Android actual implementation**

```kotlin
// shared/src/androidMain/kotlin/com/budmash/capture/ImageCapture.android.kt
package com.budmash.capture

actual class ImageCapture {
    actual fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized"))
            return
        }
        launcher.launchCamera(onResult)
    }

    actual fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit) {
        val launcher = ImageCaptureContext.launcher
        if (launcher == null) {
            onResult(ImageCaptureResult.Error("ImageCapture not initialized"))
            return
        }
        launcher.launchGallery(onResult)
    }
}

actual interface ImageCaptureLauncher {
    actual fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    actual fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
```

**Step 3: Create iOS stub**

```kotlin
// shared/src/iosMain/kotlin/com/budmash/capture/ImageCapture.ios.kt
package com.budmash.capture

actual class ImageCapture {
    actual fun captureFromCamera(onResult: (ImageCaptureResult) -> Unit) {
        onResult(ImageCaptureResult.Error("iOS not implemented"))
    }

    actual fun pickFromGallery(onResult: (ImageCaptureResult) -> Unit) {
        onResult(ImageCaptureResult.Error("iOS not implemented"))
    }
}

actual interface ImageCaptureLauncher {
    actual fun launchCamera(onResult: (ImageCaptureResult) -> Unit)
    actual fun launchGallery(onResult: (ImageCaptureResult) -> Unit)
}
```

**Step 4: Verify compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/capture/ImageCapture.kt \
        shared/src/androidMain/kotlin/com/budmash/capture/ImageCapture.android.kt \
        shared/src/iosMain/kotlin/com/budmash/capture/ImageCapture.ios.kt
git commit -m "feat: add ImageCapture expect/actual interface"
```

---

### Task 2: Implement Android ImageCaptureLauncher in MainActivity

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt`

**Step 1: Add ActivityResultContracts for camera and gallery**

Replace the entire MainActivity with:

```kotlin
package com.budmash

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.budmash.capture.ImageCaptureContext
import com.budmash.capture.ImageCaptureLauncher
import com.budmash.capture.ImageCaptureResult
import com.budmash.llm.LlmConfigStorage
import com.budmash.profile.ProfileStorageContext
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "BudMash"

class MainActivity : ComponentActivity(), ImageCaptureLauncher {

    private var cameraCallback: ((ImageCaptureResult) -> Unit)? = null
    private var galleryCallback: ((ImageCaptureResult) -> Unit)? = null
    private var photoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val callback = cameraCallback
        cameraCallback = null

        if (success && photoUri != null) {
            processImage(photoUri!!, callback)
        } else {
            callback?.invoke(ImageCaptureResult.Cancelled)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val callback = galleryCallback
        galleryCallback = null

        if (uri != null) {
            processImage(uri, callback)
        } else {
            callback?.invoke(ImageCaptureResult.Cancelled)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started")

        // Initialize contexts
        LlmConfigStorage.init(this)
        ProfileStorageContext.init(this)
        ImageCaptureContext.launcher = this
        Log.d(TAG, "All contexts initialized")

        setContent {
            App()
        }
    }

    override fun launchCamera(onResult: (ImageCaptureResult) -> Unit) {
        cameraCallback = onResult
        val photoFile = File(cacheDir, "menu_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    override fun launchGallery(onResult: (ImageCaptureResult) -> Unit) {
        galleryCallback = onResult
        pickImageLauncher.launch("image/*")
    }

    private fun processImage(uri: Uri, callback: ((ImageCaptureResult) -> Unit)?) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                callback?.invoke(ImageCaptureResult.Error("Failed to decode image"))
                return
            }

            // Scale down if too large (max 1920px on longest side)
            val scaledBitmap = scaleBitmap(bitmap, 1920)

            // Convert to base64 JPEG
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            Log.d(TAG, "Image processed: ${scaledBitmap.width}x${scaledBitmap.height}, base64 length: ${base64.length}")

            callback?.invoke(ImageCaptureResult.Success(base64, scaledBitmap.width, scaledBitmap.height))

            if (scaledBitmap != bitmap) scaledBitmap.recycle()
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            callback?.invoke(ImageCaptureResult.Error("Failed to process image: ${e.message}"))
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
```

**Step 2: Add FileProvider to AndroidManifest.xml**

In `composeApp/src/androidMain/AndroidManifest.xml`, add inside `<application>`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**Step 3: Create file_paths.xml**

Create: `composeApp/src/androidMain/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache" path="." />
</paths>
```

**Step 4: Verify compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt \
        composeApp/src/androidMain/AndroidManifest.xml \
        composeApp/src/androidMain/res/xml/file_paths.xml
git commit -m "feat: implement Android camera/gallery capture"
```

---

### Task 3: Update Navigation to Use Image Bytes

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt`

**Step 1: Change Screen.Scanning to accept imageBase64**

Change:
```kotlin
data class Scanning(val url: String) : Screen()
```

To:
```kotlin
data class Scanning(val imageBase64: String) : Screen()
```

**Step 2: Update HomeScreen callback**

Change:
```kotlin
onScanClick = { url ->
    println("[BudMash] Scan clicked with URL: $url")
    parseStatus = ParseStatus.Fetching
    currentScreen = Screen.Scanning(url)
},
```

To:
```kotlin
onPhotoCapture = { imageBase64 ->
    println("[BudMash] Photo captured, base64 length: ${imageBase64.length}")
    parseStatus = ParseStatus.Fetching
    currentScreen = Screen.Scanning(imageBase64)
},
```

**Step 3: Update LaunchedEffect in Scanning screen**

Change:
```kotlin
LaunchedEffect(screen.url) {
    println("[BudMash] LaunchedEffect: Starting parse for ${screen.url}")
    parser.parseMenu(screen.url).collect { status ->
```

To:
```kotlin
LaunchedEffect(screen.imageBase64) {
    println("[BudMash] LaunchedEffect: Starting parse for image")
    parser.parseFromImage(screen.imageBase64).collect { status ->
```

**Step 4: Update DispensaryMenu url field usage**

In Dashboard screen, the menu.url can be set to "Photo capture" since we no longer have a URL.

**Step 5: Verify compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`
Expected: Compilation errors (expected - HomeScreen and parser changes pending)

**Step 6: Commit (if compiles) or continue to next task**

---

### Task 4: Update HomeScreen UI

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/screens/HomeScreen.kt`

**Step 1: Replace URL input with photo capture buttons**

Replace the entire HomeScreen function:

```kotlin
package com.budmash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budmash.capture.ImageCapture
import com.budmash.capture.ImageCaptureResult

@Composable
fun HomeScreen(
    hasApiKey: Boolean = false,
    onPhotoCapture: (String) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val imageCapture = remember { ImageCapture() }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleCaptureResult(result: ImageCaptureResult) {
        isCapturing = false
        when (result) {
            is ImageCaptureResult.Success -> {
                errorMessage = null
                onPhotoCapture(result.base64Image)
            }
            is ImageCaptureResult.Error -> {
                errorMessage = result.message
            }
            is ImageCaptureResult.Cancelled -> {
                // User cancelled, do nothing
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Settings button in top-right corner
        TextButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BudMash",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Take a photo of a dispensary menu to find your matches",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // API Key warning
            if (!hasApiKey) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "API key not set. ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(
                            onClick = onSettingsClick,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Configure in Settings",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Take Photo button
            Button(
                onClick = {
                    isCapturing = true
                    errorMessage = null
                    imageCapture.captureFromCamera { result ->
                        handleCaptureResult(result)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCapturing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isCapturing) "Capturing..." else "Take Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Choose Photo button
            OutlinedButton(
                onClick = {
                    isCapturing = true
                    errorMessage = null
                    imageCapture.pickFromGallery { result ->
                        handleCaptureResult(result)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCapturing
            ) {
                Text(
                    text = "Choose from Gallery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Your profile builds as you mark strains you've tried.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }
    }
}
```

**Step 2: Verify compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`
Expected: May have errors (App.kt needs update)

**Step 3: Commit after App.kt is also updated**

---

### Task 5: Update DefaultMenuParser to Accept Image Bytes

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt`
- Modify: `shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt`

**Step 1: Add parseFromImage to MenuParser interface**

Add to MenuParser.kt:

```kotlin
fun parseFromImage(imageBase64: String): Flow<ParseStatus>
```

**Step 2: Implement parseFromImage in DefaultMenuParser**

Add new function (keep parseMenu for backwards compatibility if needed, or remove):

```kotlin
override fun parseFromImage(imageBase64: String): Flow<ParseStatus> = flow {
    println("[BudMash] DefaultMenuParser starting for image, base64 length: ${imageBase64.length}")

    emit(ParseStatus.Fetching)
    emit(ParseStatus.FetchComplete(imageBase64.length))

    // Step 1: Extract strains via vision LLM
    println("[BudMash] Sending image to vision LLM for extraction...")
    val strainsResult = visionExtractor.extractFromScreenshot(imageBase64, config)

    if (strainsResult.isFailure) {
        emit(ParseStatus.Error(ParseError.LlmError(strainsResult.exceptionOrNull()?.message ?: "Vision extraction failed")))
        return@flow
    }

    var strains = strainsResult.getOrThrow()
    println("[BudMash] Vision extracted ${strains.size} strains")
    emit(ParseStatus.ProductsFound(strains.size, strains.size))

    if (strains.isEmpty()) {
        emit(ParseStatus.Error(ParseError.NoFlowersFound))
        return@flow
    }

    // Step 2: Resolve terpenes for each strain
    strains = terpeneResolver.resolveAll(strains, config) { current, total ->
        println("[BudMash] Resolving terpenes: $current/$total")
    }

    strains.forEachIndexed { index, _ ->
        emit(ParseStatus.ResolvingTerpenes(index + 1, strains.size))
        delay(50)
    }

    // Step 3: Build and return menu
    val menu = DispensaryMenu(
        url = "Photo capture",
        fetchedAt = Clock.System.now().toEpochMilliseconds(),
        strains = strains
    )

    println("[BudMash] DefaultMenuParser complete with ${strains.size} strains")
    emit(ParseStatus.Complete(menu))
}
```

**Step 3: Remove ScreenshotCapture dependency**

Remove the `screenshotCapture` property from DefaultMenuParser since it's no longer needed.

**Step 4: Verify compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/budmash/parser/MenuParser.kt \
        shared/src/commonMain/kotlin/com/budmash/parser/DefaultMenuParser.kt
git commit -m "feat: add parseFromImage to accept direct image bytes"
```

---

### Task 6: Complete App.kt Integration

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt`

**Step 1: Update full App.kt with all changes**

Key changes:
1. `Screen.Scanning(val imageBase64: String)` instead of `url: String`
2. HomeScreen callback is `onPhotoCapture` instead of `onScanClick`
3. LaunchedEffect calls `parser.parseFromImage(screen.imageBase64)`

**Step 2: Remove ScreenshotCaptureContext initialization from MainActivity**

Since we no longer use WebView screenshots, remove the line:
```kotlin
ScreenshotCaptureContext.init(this)
```

**Step 3: Verify full compilation**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/budmash/ui/App.kt \
        composeApp/src/androidMain/kotlin/com/budmash/MainActivity.kt
git commit -m "feat: integrate photo capture into app navigation"
```

---

### Task 7: Test End-to-End

**Steps:**

1. Install app on device: `adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk`

2. Open app, verify HomeScreen shows "Take Photo" and "Choose from Gallery" buttons

3. Tap "Take Photo", take a photo of a menu (physical or screen)

4. Verify extraction flow:
   - Progress indicators show
   - Strains are extracted
   - Dashboard displays results

5. Test "Choose from Gallery" with a saved screenshot

**Expected behavior:**
- Camera opens when tapping "Take Photo"
- Gallery picker opens when tapping "Choose from Gallery"
- Image is processed and sent to Gemini Flash
- Strains are extracted and displayed on Dashboard

---

### Task 8: Cleanup (Optional)

**Files to consider removing:**
- `shared/src/androidMain/kotlin/com/budmash/capture/ScreenshotCapture.android.kt`
- `shared/src/iosMain/kotlin/com/budmash/capture/ScreenshotCapture.ios.kt`
- `shared/src/commonMain/kotlin/com/budmash/capture/ScreenshotCapture.kt`

Only remove after confirming new implementation works fully.

```bash
git rm shared/src/*/kotlin/com/budmash/capture/ScreenshotCapture*.kt
git commit -m "chore: remove unused WebView screenshot capture code"
```
