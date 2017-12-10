package com.airbnb.lottie.model.animatable;

import android.graphics.Color;
import android.support.annotation.IntRange;
import android.util.JsonReader;
import android.util.JsonToken;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.animation.Keyframe;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.GradientColorKeyframeAnimation;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.utils.MiscUtils;

import org.json.JSONArray;

import java.io.IOException;
import java.util.List;

public class AnimatableGradientColorValue extends BaseAnimatableValue<GradientColor,
    GradientColor> {
  private AnimatableGradientColorValue(
      List<Keyframe<GradientColor>> keyframes) {
    super(keyframes);
  }

  @Override public BaseKeyframeAnimation<GradientColor, GradientColor> createAnimation() {
    return new GradientColorKeyframeAnimation(keyframes);
  }

  public static final class Factory {
    private Factory() {
    }

    public static AnimatableGradientColorValue newInstance(
        JsonReader reader, LottieComposition composition) throws IOException {
      // TODO (json)
      // int points = json.optInt("p", json.optJSONArray("k").length() / 4);
      return new AnimatableGradientColorValue(
          AnimatableValueParser.newInstance(reader, 1, composition, new ValueFactory(2 /* points */))
      );
    }
  }

  private static class ValueFactory implements AnimatableValue.Factory<GradientColor> {
    /** The number of colors if it exists in the json or -1 if it doesn't (legacy bodymovin) */
    private final int colorPoints;

    private ValueFactory(int colorPoints) {
      this.colorPoints = colorPoints;
    }

    /**
     * Both the color stops and opacity stops are in the same array.
     * There are {@link #colorPoints} colors sequentially as:
     * [
     *     ...,
     *     position,
     *     red,
     *     green,
     *     blue,
     *     ...
     * ]
     *
     * The remainder of the array is the opacity stops sequentially as:
     * [
     *     ...,
     *     position,
     *     opacity,
     *     ...
     * ]
     */
    @Override public GradientColor valueFromObject(JsonReader reader, float scale)
        throws IOException {
      reader.beginArray();
      // STOPSHIP (json)
      while (reader.peek() != JsonToken.END_ARRAY) {
        reader.nextDouble();
      }
      reader.endArray();

      float[] positions = new float[colorPoints];
      int[] colors = new int[colorPoints];
      GradientColor gradientColor = new GradientColor(positions, colors);
      // int r = 0;
      // int g = 0;
      // if (array.length() != colorPoints * 4) {
      //   Log.w(L.TAG, "Unexpected gradient length: " + array.length() +
      //       ". Expected " + (colorPoints * 4) + ". This may affect the appearance of the gradient. " +
      //       "Make sure to save your After Effects file before exporting an animation with " +
      //       "gradients.");
      // }
      // for (int i = 0; i < colorPoints * 4; i++) {
      //   int colorIndex = i / 4;
      //   double value = array.optDouble(i);
      //   switch (i % 4) {
      //     case 0:
      //       // position
      //       positions[colorIndex] = (float) value;
      //       break;
      //     case 1:
      //       r = (int) (value * 255);
      //       break;
      //     case 2:
      //       g = (int) (value * 255);
      //       break;
      //     case 3:
      //       int b = (int) (value * 255);
      //       colors[colorIndex] = Color.argb(255, r, g, b);
      //       break;
      //   }
      // }
      //
      // addOpacityStopsToGradientIfNeeded(gradientColor, array);
      return gradientColor;
    }

    /**
     * This cheats a little bit.
     * Opacity stops can be at arbitrary intervals independent of color stops.
     * This uses the existing color stops and modifies the opacity at each existing color stop
     * based on what the opacity would be.
     *
     * This should be a good approximation is nearly all cases. However, if there are many more
     * opacity stops than color stops, information will be lost.
     */
    private void addOpacityStopsToGradientIfNeeded(GradientColor gradientColor, JSONArray array) {
      int startIndex = colorPoints * 4;
      if (array.length() <= startIndex) {
        return;
      }

      int opacityStops = (array.length() - startIndex) / 2;
      double[] positions = new double[opacityStops];
      double[] opacities = new double[opacityStops];

      for (int i = startIndex, j = 0; i < array.length(); i++) {
        if (i % 2 == 0) {
          positions[j] = array.optDouble(i);
        } else {
          opacities[j] = array.optDouble(i);
          j++;
        }
      }

      for (int i = 0; i < gradientColor.getSize(); i++) {
        int color = gradientColor.getColors()[i];
        color = Color.argb(
            getOpacityAtPosition(gradientColor.getPositions()[i], positions, opacities),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        );
        gradientColor.getColors()[i] = color;
      }
    }

    @IntRange(from=0, to=255)
    private int getOpacityAtPosition(double position, double[] positions, double[] opacities) {
      for (int i = 1; i < positions.length; i++) {
        double lastPosition = positions[i - 1];
        double thisPosition = positions[i];
        if (positions[i] >= position) {
          double progress = (position - lastPosition) / (thisPosition - lastPosition);
          return (int) (255 * MiscUtils.lerp(opacities[i - 1], opacities[i], progress));
        }
      }
      return (int) (255 * opacities[opacities.length - 1]);
    }
  }
}
