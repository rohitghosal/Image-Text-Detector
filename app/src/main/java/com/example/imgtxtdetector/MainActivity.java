package com.example.imgtxtdetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    Button chose;
    TextView resultTv;
    ImageView imgView;

    public static final int PIC_IMAGE = 121;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chose= findViewById(R.id.button);
        chose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Image"),PIC_IMAGE);
            }
        });
        resultTv= findViewById(R.id.textView);
        imgView=findViewById(R.id.imageView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PIC_IMAGE){
            imgView.setImageURI(data.getData());

            FirebaseVisionImage image;
            try {
                image = FirebaseVisionImage.fromFilePath(getApplicationContext(), data.getData());
                FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                        .getOnDeviceTextRecognizer();

                textRecognizer.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText result) {
                                // Task completed successfully
                                // ...
                                resultTv.setText(result.getText());


//                                String resultText = result.getText();
//                                for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
//                                    String blockText = block.getText();
//                                    Float blockConfidence = block.getConfidence();
//                                    List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//                                    Point[] blockCornerPoints = block.getCornerPoints();
//                                    Rect blockFrame = block.getBoundingBox();
//                                    for (FirebaseVisionText.Line line: block.getLines()) {
//                                        String lineText = line.getText();
//                                        Float lineConfidence = line.getConfidence();
//                                        List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//                                        Point[] lineCornerPoints = line.getCornerPoints();
//                                        Rect lineFrame = line.getBoundingBox();
//                                        for (FirebaseVisionText.Element element: line.getElements()) {
//                                            resultTv.append(element.getText()+" ");
//                                            String elementText = element.getText();
//                                            Float elementConfidence = element.getConfidence();
//                                            List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//                                            Point[] elementCornerPoints = element.getCornerPoints();
//                                            Rect elementFrame = element.getBoundingBox();
//                                        }
//                                        resultTv.append("\n");
//                                    }
//                                    resultTv.append("\n\n");
//                                }
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}