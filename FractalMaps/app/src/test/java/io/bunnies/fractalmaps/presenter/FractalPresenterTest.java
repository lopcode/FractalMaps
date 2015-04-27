package io.bunnies.fractalmaps.presenter;

import android.graphics.Matrix;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import io.bunnies.fractalmaps.BuildConfig;
import io.bunnies.fractalmaps.Constants;
import io.bunnies.fractalmaps.IFractalSceneDelegate;
import io.bunnies.fractalmaps.compute.FractalComputeArguments;
import io.bunnies.fractalmaps.compute.strategies.IFractalComputeStrategy;
import io.bunnies.fractalmaps.touch.IFractalTouchHandler;
import io.bunnies.fractalmaps.view.IFractalView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class FractalPresenterTest {
    private FractalPresenter presenter;
    private IFractalSceneDelegate sceneDelegate;
    private IFractalComputeStrategy computeStrategy;
    private IFractalTouchHandler touchHandler;
    private IFractalView view;

    private static final int VIEW_WIDTH = 100;
    private static final int VIEW_HEIGHT = 100;

    @Before
    public void setup() {
        this.sceneDelegate = mock(IFractalSceneDelegate.class);
        this.computeStrategy = mock(IFractalComputeStrategy.class);
        this.touchHandler = mock(IFractalTouchHandler.class);
        this.view = mock(IFractalView.class);
        this.presenter = new FractalPresenter(RuntimeEnvironment.application, this.sceneDelegate, this.computeStrategy);
        this.presenter.setTouchHandler(this.touchHandler);
        this.presenter.setView(this.view, new Matrix(), this.presenter);
        this.presenter.onViewResized(this.view, VIEW_WIDTH, VIEW_HEIGHT);
    }

    @Test
    public void testViewReadyFired() {
        // Already done once in @Before
        // this.presenter.onViewResized(this.view, VIEW_WIDTH, VIEW_HEIGHT);

        verify(this.sceneDelegate).onFractalViewReady(this.presenter);
    }

    @Test
    public void testBuffersInitialised() {
        int[] pixelBuffer = this.presenter.getPixelBuffer();
        assertEquals(pixelBuffer.length, VIEW_WIDTH * VIEW_HEIGHT);
    }

    private int countInstancesOfValue(int[] array, int value) {
        int instances = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                instances++;
            }
        }

        return instances;
    }

    @Test
    public void testTranslatePixelBuffer() {
        int[] pixelBuffer = this.presenter.getPixelBuffer();

        int zeroes = countInstancesOfValue(pixelBuffer, 0);
        int total = VIEW_WIDTH * VIEW_HEIGHT;

        pixelBuffer[0] = 123;
        pixelBuffer[VIEW_WIDTH - 1] = 456;

        this.presenter.translatePixelBuffer(5, 5);
        pixelBuffer = this.presenter.getPixelBuffer();

        assertEquals(zeroes, total);
        assertEquals(pixelBuffer[0], 0);

        assertEquals(countInstancesOfValue(pixelBuffer, 123), 1);
        assertEquals(countInstancesOfValue(pixelBuffer, 0), total - 1);
        assertEquals(pixelBuffer[5 + (VIEW_WIDTH * 5)], 123);
        assertEquals(pixelBuffer[VIEW_WIDTH - 1], 0);
    }

    @Test
    public void testRecomputeGraph() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        this.presenter.recomputeGraph(1);

        verify(this.sceneDelegate).setRenderingStatus(this.presenter, true);
        verify(this.sceneDelegate).onFractalRecomputeScheduled(this.presenter);

        ArgumentCaptor<FractalComputeArguments> argument = ArgumentCaptor.forClass(FractalComputeArguments.class);
        verify(this.computeStrategy).computeFractal(argument.capture());

        FractalComputeArguments arguments = argument.getValue();
        assertEquals(arguments.viewWidth, VIEW_WIDTH);
        assertEquals(arguments.viewHeight, VIEW_HEIGHT);
        assertEquals(arguments.pixelBlockSize, 1);
        assertEquals(arguments.xMin, Constants.testGraphPointOne[0], 0.01f);
        assertEquals(arguments.yMax, Constants.testGraphPointOne[1], 0.01f);
        assertTrue(arguments.linesPerProgressUpdate > 1);
    }

    @Test
    public void testShiftGraphArea() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        double pixelSize = this.presenter.getPixelSize(VIEW_WIDTH, Constants.testGraphPointOne);

        this.presenter.translateGraphArea(5, 5);

        double[] expectedGraphArea = Constants.testGraphPointOne;
        expectedGraphArea[0] -= (5 * pixelSize);
        expectedGraphArea[1] -= -(5 * pixelSize);

        double[] newGraphArea = this.presenter.getGraphArea();
        assertEquals(expectedGraphArea[0], newGraphArea[0], 0.01f);
        assertEquals(expectedGraphArea[1], newGraphArea[1], 0.01f);
    }

    @Test
    public void testComputeGraphAreaNow() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);

        this.presenter.computeGraphAreaNow(Constants.testGraphPointOne);

        double[] newGraphArea = this.presenter.getGraphArea();
        assertEquals(Constants.testGraphPointOne[0], newGraphArea[0], 0.01f);
        assertEquals(Constants.testGraphPointOne[1], newGraphArea[1], 0.01f);

        verify(this.sceneDelegate).scheduleRecomputeBasedOnPreferences(this.presenter, true);
    }

    @Test
    public void testNotifyRecomputeComplete() {
        this.presenter.notifyRecomputeComplete(1, 1.0);

        verify(this.sceneDelegate).setRenderingStatus(this.presenter, false);
        verify(this.sceneDelegate).onFractalRecomputed(this.presenter, 1.0);
    }

    @Test
    public void testGraphPositionTranslation() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        double[] graphPosition = this.presenter.getGraphPositionFromClickedPosition(0, 0);
        double[] pointPosition = this.presenter.getPointFromGraphPosition(graphPosition[0], graphPosition[1]);

        assertEquals(graphPosition[0], Constants.testGraphPointOne[0], 0.01f);
        assertEquals(graphPosition[1], Constants.testGraphPointOne[1], 0.01f);

        assertEquals(pointPosition[0], 0, 0.01f);
        assertEquals(pointPosition[1], 0, 0.01f);
    }

    @Test
    public void testPostUpdate() {
        int[] pixels = new int[VIEW_WIDTH * VIEW_HEIGHT];
        int[] pixelSizes = new int[VIEW_WIDTH * VIEW_HEIGHT];

        this.presenter.postUpdate(pixels, pixelSizes);

        verify(this.view).setBitmapPixels(pixels);
        verify(this.view).postThreadSafeRedraw();
    }

    @Test
    public void testPostFinished() {
        int[] pixels = new int[VIEW_WIDTH * VIEW_HEIGHT];
        int[] pixelSizes = new int[VIEW_WIDTH * VIEW_HEIGHT];

        this.presenter.postFinished(pixels, pixelSizes, 1, 1.0f);

        verify(this.view).setBitmapPixels(pixels);
        verify(this.view).postThreadSafeRedraw();
        verify(this.sceneDelegate).setRenderingStatus(this.presenter, false);
    }

    @Test
    public void testOnComputeStarted() {
        this.presenter.onComputeStarted(1);

        verify(this.sceneDelegate).setRenderingStatus(this.presenter, true);
    }

    @Test
    public void testStartDraggingFractal() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        this.presenter.startDraggingFractal();

        verify(this.computeStrategy).stopAllRendering();
        verify(this.view, times(2)).setFractalTransformMatrix(any(Matrix.class));
    }

    @Test
    public void testDragFractal() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        this.presenter.dragFractal(1f, 2f, 3f, 4f);

        // Called once in @Before, once now
        verify(this.view, times(2)).setFractalTransformMatrix(any(Matrix.class));
        verify(this.view).postUIThreadRedraw();
    }

    @Test
    public void testStopDraggingFractal() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        this.presenter.stopDraggingFractal(false, 1f, 2f);

        verify(this.view).setBitmapPixels(this.presenter.getPixelBuffer());
        verify(this.sceneDelegate).scheduleRecomputeBasedOnPreferences(this.presenter, false);
        verify(this.view, times(2)).setFractalTransformMatrix(any(Matrix.class));
        verify(this.view).postUIThreadRedraw();
    }

    @Test
    public void testStopScalingFractal() {
        this.presenter.stopScalingFractal();

        verify(this.view).cacheCurrentBitmap(this.presenter.getPixelBuffer());
    }

    @Test
    public void testScaleFractal() {
        this.presenter.setGraphArea(Constants.testGraphPointOne);
        this.presenter.scaleFractal(1.0f, 50f, 50f);

        verify(this.view).setFractalTransformMatrix(any(Matrix.class));
        verify(this.view).postUIThreadRedraw();
    }

    @Test
    public void testLongClick() {
        this.presenter.onLongClick(5f, 6f);

        verify(this.sceneDelegate).onFractalLongClick(this.presenter, 5f, 6f);
    }
}
