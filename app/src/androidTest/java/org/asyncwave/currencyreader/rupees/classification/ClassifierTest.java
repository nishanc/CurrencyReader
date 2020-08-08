/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.asyncwave.currencyreader.rupees.classification;

import static com.google.common.truth.Truth.assertThat;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import org.asyncwave.currencyreader.rupees.classification.tflite.Classifier;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Golden test for Image Classification Reference app. */
@RunWith(AndroidJUnit4.class)
public class ClassifierTest {

  @Rule
  public ActivityTestRule<ClassifierActivity> rule =
      new ActivityTestRule<>(ClassifierActivity.class);

  private static final String[] INPUTS = {"fox.jpg"};
  private static final String[] GOLDEN_OUTPUTS = {"fox-mobilenet_v1_1.0_224.txt"};

  @Test
  public void classificationResultsShouldNotChange() throws IOException {
    ClassifierActivity activity = rule.getActivity();
    Classifier classifier = Classifier.create(activity, Classifier.Model.FLOAT_MOBILENET, Classifier.Device.CPU, 1);
    for (int i = 0; i < INPUTS.length; i++) {
      String imageFileName = INPUTS[i];
      String goldenOutputFileName = GOLDEN_OUTPUTS[i];
      Bitmap input = loadImage(imageFileName);
      List<Classifier.Recognition> goldenOutput = loadRecognitions(goldenOutputFileName);

      List<Classifier.Recognition> result = classifier.recognizeImage(input, 0);
      Iterator<Classifier.Recognition> goldenOutputIterator = goldenOutput.iterator();

      for (Classifier.Recognition actual : result) {
        Assert.assertTrue(goldenOutputIterator.hasNext());
        Classifier.Recognition expected = goldenOutputIterator.next();
        assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
        assertThat(actual.getConfidence()).isWithin(0.01f).of(expected.getConfidence());
      }
    }
  }

  private static Bitmap loadImage(String fileName) {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    InputStream inputStream = null;
    try {
      inputStream = assetManager.open(fileName);
    } catch (IOException e) {
      Log.e("Test", "Cannot load image from assets");
    }
    return BitmapFactory.decodeStream(inputStream);
  }

  private static List<Classifier.Recognition> loadRecognitions(String fileName) {
    AssetManager assetManager =
        InstrumentationRegistry.getInstrumentation().getContext().getAssets();
    InputStream inputStream = null;
    try {
      inputStream = assetManager.open(fileName);
    } catch (IOException e) {
      Log.e("Test", "Cannot load probability results from assets");
    }
    Scanner scanner = new Scanner(inputStream);
    List<Classifier.Recognition> result = new ArrayList<>();
    while (scanner.hasNext()) {
      String category = scanner.next();
      category = category.replace('_', ' ');
      if (!scanner.hasNextFloat()) {
        break;
      }
      float probability = scanner.nextFloat();
      Classifier.Recognition recognition = new Classifier.Recognition(null, category, probability, null);
      result.add(recognition);
    }
    return result;
  }
}
