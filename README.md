# ImageText-Detector

This andriod app using java is for detection of text in images and extract it out

You can use ML Kit to recognize text in images. ML Kit has both a general-purpose API suitable for recognizing text in images, such as the text of a street sign, and an API optimized for recognizing the text of documents. The general-purpose API has both on-device and cloud-based models. Document text recognition is available only as a cloud-based model. See the overview for a comparison of the cloud and on-device models.

# Before you begin
If you haven't already, add Firebase to your Android project.
In your project-level build.gradle file, make sure to include Google's Maven repository in both your buildscript and allprojects sections.
Add the dependencies for the ML Kit Android libraries to your module (app-level) Gradle file (usually app/build.gradle):
apply plugin: 'com.android.application'
apply plugin: 'com.google.gms.google-services'

dependencies {
  // ...

  implementation 'com.google.firebase:firebase-ml-vision:24.0.3'
}

Optional but recommended: If you use the on-device API, configure your app to automatically download the ML model to the device after your app is installed from the Play Store.
To do so, add the following declaration to your app's AndroidManifest.xml file:

<application ...>
  ...
  <meta-data
      android:name="com.google.firebase.ml.vision.DEPENDENCIES"
      android:value="ocr" />
  <!-- To use multiple models: android:value="ocr,model2,model3" -->
</application>

If you do not enable install-time model downloads, the model will be downloaded the first time you run the on-device detector. Requests you make before the download has completed will produce no results.
If you want to use the Cloud-based model, and you have not already enabled the Cloud-based APIs for your project, do so now:

Open the ML Kit APIs page of the Firebase console.
If you have not already upgraded your project to a Blaze plan, click Upgrade to do so. (You will be prompted to upgrade only if your project isn't on the Blaze plan.)

Only Blaze-level projects can use Cloud-based APIs.

If Cloud-based APIs aren't already enabled, click Enable Cloud-based APIs.
Before you deploy to production an app that uses a Cloud API, you should take some additional steps to prevent and mitigate the effect of unauthorized API access.
If you want to use only the on-device model, you can skip this step.

Now you are ready to start recognizing text in images.

# Input image guidelines
For ML Kit to accurately recognize text, input images must contain text that is represented by sufficient pixel data. Ideally, for Latin text, each character should be at least 16x16 pixels. For Chinese, Japanese, and Korean text (only supported by the cloud-based APIs), each character should be 24x24 pixels. For all languages, there is generally no accuracy benefit for characters to be larger than 24x24 pixels.

So, for example, a 640x480 image might work well to scan a business card that occupies the full width of the image. To scan a document printed on letter-sized paper, a 720x1280 pixel image might be required.

Poor image focus can hurt text recognition accuracy. If you aren't getting acceptable results, try asking the user to recapture the image.

If you are recognizing text in a real-time application, you might also want to consider the overall dimensions of the input images. Smaller images can be processed faster, so to reduce latency, capture images at lower resolutions (keeping in mind the above accuracy requirements) and ensure that the text occupies as much of the image as possible. Also see Tips to improve real-time performance.

# Recognize text in images
To recognize text in an image using either an on-device or cloud-based model, run the text recognizer as described below.

1. Run the text recognizer
To recognize text in an image, create a FirebaseVisionImage object from either a Bitmap, media.Image, ByteBuffer, byte array, or a file on the device. Then, pass the FirebaseVisionImage object to the FirebaseVisionTextRecognizer's processImage method.
Create a FirebaseVisionImage object from your image.

To create a FirebaseVisionImage object from a media.Image object, such as when capturing an image from a device's camera, pass the media.Image object and the image's rotation to FirebaseVisionImage.fromMediaImage().

If you use the CameraX library, the OnImageCapturedListener and ImageAnalysis.Analyzer classes calculate the rotation value for you, so you just need to convert the rotation to one of ML Kit's ROTATION_ constants before calling FirebaseVisionImage.fromMediaImage():

Java
Kotlin+KTX
private class YourAnalyzer implements ImageAnalysis.Analyzer {

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }

    @Override
    public void analyze(ImageProxy imageProxy, int degrees) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }
        Image mediaImage = imageProxy.getImage();
        int rotation = degreesToFirebaseRotation(degrees);
        FirebaseVisionImage image =
                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
        // Pass image to an ML Kit Vision API
        // ...
    }
}

If you don't use a camera library that gives you the image's rotation, you can calculate it from the device's rotation and the orientation of camera sensor in the device:

Java
Kotlin+KTX
private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
}

/**
 * Get the angle by which an image must be rotated given the device's current
 * orientation.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private int getRotationCompensation(String cameraId, Activity activity, Context context)
        throws CameraAccessException {
    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int rotationCompensation = ORIENTATIONS.get(deviceRotation);

    // On most devices, the sensor orientation is 90 degrees, but for some
    // devices it is 270 degrees. For devices with a sensor orientation of
    // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
    CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
    int sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION);
    rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

    // Return the corresponding FirebaseVisionImageMetadata rotation value.
    int result;
    switch (rotationCompensation) {
        case 0:
            result = FirebaseVisionImageMetadata.ROTATION_0;
            break;
        case 90:
            result = FirebaseVisionImageMetadata.ROTATION_90;
            break;
        case 180:
            result = FirebaseVisionImageMetadata.ROTATION_180;
            break;
        case 270:
            result = FirebaseVisionImageMetadata.ROTATION_270;
            break;
        default:
            result = FirebaseVisionImageMetadata.ROTATION_0;
            Log.e(TAG, "Bad rotation value: " + rotationCompensation);
    }
    return result;
}

Then, pass the media.Image object and the rotation value to FirebaseVisionImage.fromMediaImage():

Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

To create a FirebaseVisionImage object from a file URI, pass the app context and file URI to FirebaseVisionImage.fromFilePath(). This is useful when you use an ACTION_GET_CONTENT intent to prompt the user to select an image from their gallery app.
Java
Kotlin+KTX
FirebaseVisionImage image;
try {
    image = FirebaseVisionImage.fromFilePath(context, uri);
} catch (IOException e) {
    e.printStackTrace();
}

To create a FirebaseVisionImage object from a ByteBuffer or a byte array, first calculate the image rotation as described above for media.Image input.
Then, create a FirebaseVisionImageMetadata object that contains the image's height, width, color encoding format, and rotation:

Java
Kotlin+KTX
FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
        .setWidth(480)   // 480x360 is typically sufficient for
        .setHeight(360)  // image recognition
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setRotation(rotation)
        .build();

Use the buffer or array, and the metadata object, to create a FirebaseVisionImage object:

Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);
// Or: FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(byteArray, metadata);

To create a FirebaseVisionImage object from a Bitmap object:
Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

The image represented by the Bitmap object must be upright, with no additional rotation required.
Get an instance of FirebaseVisionTextRecognizer.

# To use the on-device model:

Java
Kotlin+KTX
FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
        .getOnDeviceTextRecognizer();

To use the cloud-based model:

Java
Kotlin+KTX
FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
        .getCloudTextRecognizer();
// Or, to change the default settings:
//   FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
//          .getCloudTextRecognizer(options);

// Or, to provide language hints to assist with language detection:
// See https://cloud.google.com/vision/docs/languages for supported languages
FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
        .setLanguageHints(Arrays.asList("en", "hi"))
        .build();

Use of ML Kit to access Cloud ML functionality is subject to the Google Cloud Platform License Agreement and Service Specific Terms, and billed accordingly. For billing information, see the Firebase Pricing page.
Finally, pass the image to the processImage method:

Java
Kotlin+KTX
Task<FirebaseVisionText> result =
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        // Task completed successfully
                        // ...
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });

# 2. Extract text from blocks of recognized text
If the text recognition operation succeeds, a FirebaseVisionText object will be passed to the success listener. A FirebaseVisionText object contains the full text recognized in the image and zero or more TextBlock objects.
Each TextBlock represents a rectangular block of text, which contains zero or more Line objects. Each Line object contains zero or more Element objects, which represent words and word-like entities (dates, numbers, and so on).

For each TextBlock, Line, and Element object, you can get the text recognized in the region and the bounding coordinates of the region.

For example:

Java
Kotlin+KTX
String resultText = result.getText();
for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
    String blockText = block.getText();
    Float blockConfidence = block.getConfidence();
    List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
    Point[] blockCornerPoints = block.getCornerPoints();
    Rect blockFrame = block.getBoundingBox();
    for (FirebaseVisionText.Line line: block.getLines()) {
        String lineText = line.getText();
        Float lineConfidence = line.getConfidence();
        List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
        Point[] lineCornerPoints = line.getCornerPoints();
        Rect lineFrame = line.getBoundingBox();
        for (FirebaseVisionText.Element element: line.getElements()) {
            String elementText = element.getText();
            Float elementConfidence = element.getConfidence();
            List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
            Point[] elementCornerPoints = element.getCornerPoints();
            Rect elementFrame = element.getBoundingBox();
        }
    }
}

Note: Recognized languages are provided only when using the cloud model. To identify languages with the on-device model, use ML Kit's language identification API.
Tips to improve real-time performance
If you want use the on-device model to recognize text in a real-time application, follow these guidelines to achieve the best framerates:

Throttle calls to the text recognizer. If a new video frame becomes available while the text recognizer is running, drop the frame.
If you are using the output of the text recognizer to overlay graphics on the input image, first get the result from ML Kit, then render the image and overlay in a single step. By doing so, you render to the display surface only once for each input frame.
If you use the Camera2 API, capture images in ImageFormat.YUV_420_888 format.

If you use the older Camera API, capture images in ImageFormat.NV21 format.

Consider capturing images at a lower resolution. However, also keep in mind this API's image dimension requirements.
Next steps
Before you deploy to production an app that uses a Cloud API, you should take some additional steps to prevent and mitigate the effect of unauthorized API access.
Recognize text in images of documents
To recognize the text of a document, configure and run the cloud-based document text recognizer as described below.

Use of ML Kit to access Cloud ML functionality is subject to the Google Cloud Platform License Agreement and Service Specific Terms, and billed accordingly. For billing information, see the Firebase Pricing page.
The document text recognition API, described below, provides an interface that is intended to be more convenient for working with images of documents. However, if you prefer the interface provided by the FirebaseVisionTextRecognizer API, you can use it instead to scan documents by configuring the cloud text recognizer to use the dense text model.

# To use the document text recognition API:

1. Run the text recognizer
To recognize text in an image, create a FirebaseVisionImage object from either a Bitmap, media.Image, ByteBuffer, byte array, or a file on the device. Then, pass the FirebaseVisionImage object to the FirebaseVisionDocumentTextRecognizer's processImage method.
Create a FirebaseVisionImage object from your image.

To create a FirebaseVisionImage object from a media.Image object, such as when capturing an image from a device's camera, pass the media.Image object and the image's rotation to FirebaseVisionImage.fromMediaImage().

If you use the CameraX library, the OnImageCapturedListener and ImageAnalysis.Analyzer classes calculate the rotation value for you, so you just need to convert the rotation to one of ML Kit's ROTATION_ constants before calling FirebaseVisionImage.fromMediaImage():

Java
Kotlin+KTX
private class YourAnalyzer implements ImageAnalysis.Analyzer {

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }

    @Override
    public void analyze(ImageProxy imageProxy, int degrees) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }
        Image mediaImage = imageProxy.getImage();
        int rotation = degreesToFirebaseRotation(degrees);
        FirebaseVisionImage image =
                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
        // Pass image to an ML Kit Vision API
        // ...
    }
}

If you don't use a camera library that gives you the image's rotation, you can calculate it from the device's rotation and the orientation of camera sensor in the device:

Java
Kotlin+KTX
private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
}

/**
 * Get the angle by which an image must be rotated given the device's current
 * orientation.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private int getRotationCompensation(String cameraId, Activity activity, Context context)
        throws CameraAccessException {
    // Get the device's current rotation relative to its "native" orientation.
    // Then, from the ORIENTATIONS table, look up the angle the image must be
    // rotated to compensate for the device's rotation.
    int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int rotationCompensation = ORIENTATIONS.get(deviceRotation);

    // On most devices, the sensor orientation is 90 degrees, but for some
    // devices it is 270 degrees. For devices with a sensor orientation of
    // 270, rotate the image an additional 180 ((270 + 270) % 360) degrees.
    CameraManager cameraManager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
    int sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION);
    rotationCompensation = (rotationCompensation + sensorOrientation + 270) % 360;

    // Return the corresponding FirebaseVisionImageMetadata rotation value.
    int result;
    switch (rotationCompensation) {
        case 0:
            result = FirebaseVisionImageMetadata.ROTATION_0;
            break;
        case 90:
            result = FirebaseVisionImageMetadata.ROTATION_90;
            break;
        case 180:
            result = FirebaseVisionImageMetadata.ROTATION_180;
            break;
        case 270:
            result = FirebaseVisionImageMetadata.ROTATION_270;
            break;
        default:
            result = FirebaseVisionImageMetadata.ROTATION_0;
            Log.e(TAG, "Bad rotation value: " + rotationCompensation);
    }
    return result;
}

Then, pass the media.Image object and the rotation value to FirebaseVisionImage.fromMediaImage():

Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

To create a FirebaseVisionImage object from a file URI, pass the app context and file URI to FirebaseVisionImage.fromFilePath(). This is useful when you use an ACTION_GET_CONTENT intent to prompt the user to select an image from their gallery app.
Java
Kotlin+KTX
FirebaseVisionImage image;
try {
    image = FirebaseVisionImage.fromFilePath(context, uri);
} catch (IOException e) {
    e.printStackTrace();
}

To create a FirebaseVisionImage object from a ByteBuffer or a byte array, first calculate the image rotation as described above for media.Image input.
Then, create a FirebaseVisionImageMetadata object that contains the image's height, width, color encoding format, and rotation:

Java
Kotlin+KTX
FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
        .setWidth(480)   // 480x360 is typically sufficient for
        .setHeight(360)  // image recognition
        .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        .setRotation(rotation)
        .build();

Use the buffer or array, and the metadata object, to create a FirebaseVisionImage object:

Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer, metadata);
// Or: FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(byteArray, metadata);

To create a FirebaseVisionImage object from a Bitmap object:
Java
Kotlin+KTX
FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

The image represented by the Bitmap object must be upright, with no additional rotation required.
Get an instance of FirebaseVisionDocumentTextRecognizer:

Java
Kotlin+KTX
FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
        .getCloudDocumentTextRecognizer();

// Or, to provide language hints to assist with language detection:
// See https://cloud.google.com/vision/docs/languages for supported languages
FirebaseVisionCloudDocumentRecognizerOptions options =
        new FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "hi"))
                .build();
FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
        .getCloudDocumentTextRecognizer(options);

Finally, pass the image to the processImage method:

Java
Kotlin+KTX
detector.processImage(myImage)
        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
            @Override
            public void onSuccess(FirebaseVisionDocumentText result) {
                // Task completed successfully
                // ...
            }
        })
        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Task failed with an exception
                // ...
            }
        });

2. Extract text from blocks of recognized text
If the text recognition operation succeeds, it will return a FirebaseVisionDocumentText object. A FirebaseVisionDocumentText object contains the full text recognized in the image and a hierarchy of objects that reflect the structure of the recognized document:

FirebaseVisionDocumentText.Block
FirebaseVisionDocumentText.Paragraph
FirebaseVisionDocumentText.Word
FirebaseVisionDocumentText.Symbol
For each Block, Paragraph, Word, and Symbol object, you can get the text recognized in the region and the bounding coordinates of the region.

For example:

Java
Kotlin+KTX
String resultText = result.getText();
for (FirebaseVisionDocumentText.Block block: result.getBlocks()) {
    String blockText = block.getText();
    Float blockConfidence = block.getConfidence();
    List<RecognizedLanguage> blockRecognizedLanguages = block.getRecognizedLanguages();
    Rect blockFrame = block.getBoundingBox();
    for (FirebaseVisionDocumentText.Paragraph paragraph: block.getParagraphs()) {
        String paragraphText = paragraph.getText();
        Float paragraphConfidence = paragraph.getConfidence();
        List<RecognizedLanguage> paragraphRecognizedLanguages = paragraph.getRecognizedLanguages();
        Rect paragraphFrame = paragraph.getBoundingBox();
        for (FirebaseVisionDocumentText.Word word: paragraph.getWords()) {
            String wordText = word.getText();
            Float wordConfidence = word.getConfidence();
            List<RecognizedLanguage> wordRecognizedLanguages = word.getRecognizedLanguages();
            Rect wordFrame = word.getBoundingBox();
            for (FirebaseVisionDocumentText.Symbol symbol: word.getSymbols()) {
                String symbolText = symbol.getText();
                Float symbolConfidence = symbol.getConfidence();
                List<RecognizedLanguage> symbolRecognizedLanguages = symbol.getRecognizedLanguages();
                Rect symbolFrame = symbol.getBoundingBox();
            }
        }
    }
}

# Next steps
Before you deploy to production an app that uses a Cloud API, you should take some additional steps to prevent and mitigate the effect of unauthorized API access.
