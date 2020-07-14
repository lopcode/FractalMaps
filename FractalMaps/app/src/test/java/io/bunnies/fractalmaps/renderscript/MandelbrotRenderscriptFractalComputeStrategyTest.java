package io.bunnies.fractalmaps.compute.strategies.renderscript;

import android.os.Build;
import android.util.SparseArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.bunnies.fractalmaps.Constants;
import io.bunnies.fractalmaps.compute.FractalComputeArguments;
import io.bunnies.fractalmaps.compute.IFractalComputeDelegate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Config(sdk = Build.VERSION_CODES.P)
@RunWith(RobolectricTestRunner.class)
public class MandelbrotRenderscriptFractalComputeStrategyTest {
    private MandelbrotRenderscriptFractalComputeStrategy strategy;
    private IFractalComputeDelegate delegate;

    private static final int VIEW_WIDTH = 100;
    private static final int VIEW_HEIGHT = 100;

    @Before
    public void setup() {
        this.delegate = mock(IFractalComputeDelegate.class);
        this.strategy = new MandelbrotRenderscriptFractalComputeStrategy();
        this.strategy.setContext(RuntimeEnvironment.application);
        this.strategy.initialise(VIEW_WIDTH, VIEW_HEIGHT, this.delegate);
    }

    @Test
    public void testInitialisation() {
        MandelbrotRenderscriptFractalComputeStrategy strategySpy = spy(this.strategy);
        strategySpy.initialise(VIEW_WIDTH, VIEW_HEIGHT, this.delegate);

        verify(strategySpy).initialiseRowIndexCache(anyList(), anyInt(), anyInt());
    }

    @Test
    public void testInitialiseRowIndexCache() {
        MandelbrotRenderscriptFractalComputeStrategy strategySpy = spy(this.strategy);
        strategySpy.initialiseRowIndexCache(Arrays.asList(1, 3), 1, 4);

        SparseArray<int[][]> rowIndexMap = strategySpy.rowIndices.get(32);
        int[][] indices = rowIndexMap.get(1);
        int progressUpdates = indices.length;

        Set<Integer> indexSeenMap = new HashSet<>();

        for (int i = 0; i < progressUpdates; i++) {
            int linesInProgressUpdate = indices[i].length;
            for (int j = 0; j < linesInProgressUpdate; j++) {
                indexSeenMap.add(indices[i][j]);
            }
        }

        assertEquals(indexSeenMap.size(), VIEW_HEIGHT);
    }

    @Test
    public void testTearDown() {
        MandelbrotRenderscriptFractalComputeStrategy strategySpy = spy(this.strategy);
        strategySpy.tearDown();

        verify(strategySpy).stopAllRendering();
        verify(strategySpy).interruptThreads();
        verify(strategySpy).destroyRenderscriptObjects();
    }

    @Test
    public void testComputeFractal() {
        int[] pixelBuffer = new int[VIEW_WIDTH * VIEW_HEIGHT];
        int[] pixelBufferSizes = new int[VIEW_WIDTH * VIEW_HEIGHT];

        FractalComputeArguments arguments = new FractalComputeArguments(1, 32, 32, 1, VIEW_WIDTH, VIEW_HEIGHT,
                Constants.testGraphPointOne[0], Constants.testGraphPointOne[1], Constants.testGraphPointOne[2] / VIEW_WIDTH,
                pixelBuffer, pixelBufferSizes);

        this.strategy.computeFractalWithArguments(arguments);

        // Cannot test further currently because Renderscript appears to fail to initialise in the test environment

//        verify(this.delegate).onComputeStarted(1);
//        verify(this.delegate).postUpdate(any(int[].class), any(int[].class));
//        verify(this.delegate).postFinished(any(int[].class), any(int[].class), 1, anyDouble());
    }
}
