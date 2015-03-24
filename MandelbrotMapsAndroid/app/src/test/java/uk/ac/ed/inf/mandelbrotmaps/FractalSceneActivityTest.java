package uk.ac.ed.inf.mandelbrotmaps;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;

import uk.ac.ed.inf.mandelbrotmaps.colouring.EnumColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.FractalComputeArguments;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.IFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.JuliaSeedSettable;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinColour;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinOverlay;
import uk.ac.ed.inf.mandelbrotmaps.presenter.FractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.presenter.IFractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.settings.FractalTypeEnum;
import uk.ac.ed.inf.mandelbrotmaps.settings.SceneLayoutEnum;
import uk.ac.ed.inf.mandelbrotmaps.settings.SettingsManager;
import uk.ac.ed.inf.mandelbrotmaps.settings.saved_state.SavedGraphArea;
import uk.ac.ed.inf.mandelbrotmaps.settings.saved_state.SavedJuliaGraph;
import uk.ac.ed.inf.mandelbrotmaps.view.FractalView;
import uk.ac.ed.inf.mandelbrotmaps.view.IFractalView;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@Config(constants = BuildConfig.class)
@RunWith(RobolectricGradleTestRunner.class)
public class FractalSceneActivityTest {
    private FractalSceneActivity activity;

    @Before
    public void setup() throws Exception {
        this.activity = Robolectric.buildActivity(FractalSceneActivity.class).create().get();

//        this.activity.mandelbrotStrategy = mock(IFractalComputeStrategy.class);
//        this.activity.juliaStrategy = mock(IFractalComputeStrategy.class);
//        this.activity.mandelbrotFractalView = mock(FractalView.class);
//        this.activity.juliaFractalView = mock(FractalView.class);
//        this.activity.juliaSetter = mock(JuliaSeedSettable.class);
    }

    @Test
    public void testActivityCreated() {
        assertNotNull(this.activity);
    }

    @Test
    public void testLayout() {
        assertNotNull(this.activity.findViewById(R.id.firstFractalView));
        assertNotNull(this.activity.findViewById(R.id.secondFractalView));
        assertNotNull(this.activity.findViewById(R.id.toolbar));
        assertNotNull(this.activity.findViewById(R.id.toolbarProgress));
        assertNotNull(this.activity.findViewById(R.id.toolbarTextProgress));
    }

    @Test
    public void testSaveGraphStates() {

        double[] mandelbrotGraphArea = this.activity.mandelbrotFractalPresenter.getGraphArea();
        double[] juliaGraphArea = this.activity.juliaFractalPresenter.getGraphArea();
        double[] juliaParams = this.activity.juliaSetter.getJuliaSeed();

        this.activity.settings = spy(this.activity.settings);

        this.activity.saveGraphStates();

        ArgumentCaptor<SavedGraphArea> mandelbrotArgument = ArgumentCaptor.forClass(SavedGraphArea.class);
        verify(this.activity.settings).savePreviousMandelbrotGraph(mandelbrotArgument.capture());
        SavedGraphArea savedMandelbrotArea = mandelbrotArgument.getValue();

        ArgumentCaptor<SavedJuliaGraph> juliaArgument = ArgumentCaptor.forClass(SavedJuliaGraph.class);
        verify(this.activity.settings).savePreviousJuliaGraph(juliaArgument.capture());
        SavedJuliaGraph savedJuliaGraph = juliaArgument.getValue();

        assertEquals(savedMandelbrotArea.graphX, mandelbrotGraphArea[0], 0.01f);
        assertEquals(savedMandelbrotArea.graphY, mandelbrotGraphArea[1], 0.01f);
        assertEquals(savedMandelbrotArea.graphZ, mandelbrotGraphArea[2], 0.01f);
        assertEquals(savedJuliaGraph.graphX, juliaGraphArea[0], 0.01f);
        assertEquals(savedJuliaGraph.graphY, juliaGraphArea[1], 0.01f);
        assertEquals(savedJuliaGraph.graphZ, juliaGraphArea[2], 0.01f);
        assertEquals(savedJuliaGraph.juliaSeedX, juliaParams[0], 0.01f);
        assertEquals(savedJuliaGraph.juliaSeedY, juliaParams[1], 0.01f);
    }

    @Test
    public void testOnDestroy() {
        this.activity.mandelbrotStrategy = mock(IFractalComputeStrategy.class);
        this.activity.juliaStrategy = mock(IFractalComputeStrategy.class);

        this.activity.onDestroy();

        verify(this.activity.mandelbrotStrategy).tearDown();
        verify(this.activity.juliaStrategy).tearDown();
    }

    @Test
    public void testOpenMandelbrotContextMenu() {
        MenuItem item = new RoboMenuItem(R.id.menuShowMandelbrotMenu);

        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.onOptionsItemSelected(item);

        verify(spiedActivity).onShowMandelbrotMenuClicked();
    }

    @Test
    public void testOpenJuliaContextMenu() {
        MenuItem item = new RoboMenuItem(R.id.menuShowJuliaMenu);

        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.onOptionsItemSelected(item);

        verify(spiedActivity).onShowJuliaMenuClicked();
    }

    @Test
    public void testResetMandelbrotFractalFromContextMenu() {
        MenuItem item = new RoboMenuItem(R.id.menuResetFractal);

        this.activity.viewContext = this.activity.mandelbrotFractalView;
        FractalSceneActivity spiedActivity = spy(this.activity);
        Mockito.doNothing().when(spiedActivity).resetMandelbrotFractal();
        spiedActivity.onContextItemSelected(item);

        verify(spiedActivity).onResetFractalClicked(this.activity.mandelbrotFractalView);
        verify(spiedActivity).resetMandelbrotFractal();
    }

    @Test
    public void testResetJuliaFractalFromContextMenu() {
        MenuItem item = new RoboMenuItem(R.id.menuResetFractal);

        this.activity.viewContext = this.activity.juliaFractalView;
        FractalSceneActivity spiedActivity = spy(this.activity);
        Mockito.doNothing().when(spiedActivity).resetJuliaFractal();
        spiedActivity.onContextItemSelected(item);

        verify(spiedActivity).onResetFractalClicked(this.activity.juliaFractalView);
        verify(spiedActivity).resetJuliaFractal();
    }

    @Test
    public void testResetAllFromMainMenu() {
        MenuItem item = new RoboMenuItem(R.id.menuResetAll);

        FractalSceneActivity spiedActivity = spy(this.activity);
        Mockito.doNothing().when(spiedActivity).resetMandelbrotFractal();
        Mockito.doNothing().when(spiedActivity).resetJuliaFractal();
        spiedActivity.onOptionsItemSelected(item);

        verify(spiedActivity).onResetAllClicked();
        verify(spiedActivity).resetMandelbrotFractal();
        verify(spiedActivity).resetJuliaFractal();
    }

    @Test
    public void testBenchmarks() {
        MenuItem item_one = new RoboMenuItem(R.id.menuBenchmarkOne);
        MenuItem item_two = new RoboMenuItem(R.id.menuBenchmarkTwo);
        MenuItem item_three = new RoboMenuItem(R.id.menuBenchmarkThree);
        MenuItem item_four = new RoboMenuItem(R.id.menuBenchmarkFour);
        MenuItem item_five = new RoboMenuItem(R.id.menuBenchmarkFive);
        MenuItem item_six = new RoboMenuItem(R.id.menuBenchmarkSix);
        MenuItem item_seven = new RoboMenuItem(R.id.menuBenchmarkSeven);

        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.mandelbrotFractalPresenter = mock(FractalPresenter.class);

        spiedActivity.onOptionsItemSelected(item_one);
        spiedActivity.onOptionsItemSelected(item_two);
        spiedActivity.onOptionsItemSelected(item_three);
        spiedActivity.onOptionsItemSelected(item_four);
        spiedActivity.onOptionsItemSelected(item_five);
        spiedActivity.onOptionsItemSelected(item_six);
        spiedActivity.onOptionsItemSelected(item_seven);

        verify(spiedActivity).onBenchmarkOneClicked();
        verify(spiedActivity).onBenchmarkTwoClicked();
        verify(spiedActivity).onBenchmarkThreeClicked();
        verify(spiedActivity).onBenchmarkFourClicked();
        verify(spiedActivity).onBenchmarkFiveClicked();
        verify(spiedActivity).onBenchmarkSixClicked();
        verify(spiedActivity).onBenchmarkSevenClicked();
        verify(spiedActivity.mandelbrotFractalPresenter, times(7)).computeGraphAreaNow(any(double[].class));
    }

    @Test
    public void testSwitchFractalLayout() {
        this.activity.settings = mock(SettingsManager.class);
        when(this.activity.settings.getViewsSwitched()).thenReturn(true);
        this.activity.sceneStartTime = 0;

        this.activity.onSwitchFractalViewLayoutClicked(this.activity.mandelbrotFractalView);

        verify(this.activity.settings).setViewsSwitched(anyBoolean());
    }

    @Test
    public void testChangeFractalPosition() {
        this.activity.settings = mock(SettingsManager.class);
        when(this.activity.settings.getViewsSwitched()).thenReturn(true);
        this.activity.sceneStartTime = 0;

        this.activity.onChangeFractalPositionClicked(this.activity.mandelbrotFractalView);

        verify(this.activity.settings).setViewsSwitched(false);
    }

    @Test
    public void testResetMandelbrotFractal() {
        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.mandelbrotFractalPresenter = mock(FractalPresenter.class);
        Mockito.doNothing().when(spiedActivity).onFractalViewReady(any(IFractalPresenter.class));

        spiedActivity.resetMandelbrotFractal();

        verify(spiedActivity.mandelbrotFractalPresenter).setGraphArea(MandelbrotJuliaLocation.defaultMandelbrotGraphArea);
        verify(spiedActivity).onFractalViewReady(spiedActivity.mandelbrotFractalPresenter);
    }

    @Test
    public void testRestJuliaFractal() {
        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.juliaFractalPresenter = mock(FractalPresenter.class);
        Mockito.doNothing().when(spiedActivity).onFractalViewReady(any(IFractalPresenter.class));

        spiedActivity.resetJuliaFractal();

        verify(spiedActivity.juliaFractalPresenter).setGraphArea(MandelbrotJuliaLocation.defaultJuliaGraphArea);
        verify(spiedActivity).onFractalViewReady(spiedActivity.juliaFractalPresenter);
    }

    @Test
    public void testScheduleRecomputeBasedOnPreferences() {
        IFractalPresenter presenter = mock(IFractalPresenter.class);
        when(presenter.getComputeStrategy()).thenReturn(mock(IFractalComputeStrategy.class));
        FractalSceneActivity spiedActivity = spy(this.activity);
        spiedActivity.scheduleRecomputeBasedOnPreferences(presenter, true);

        verify(presenter.getComputeStrategy()).stopAllRendering();
        verify(presenter).recomputeGraph(1);
    }

    @Test
    public void testOnPinColourChanged() {
        this.activity.mandelbrotFractalView = mock(FractalView.class);
        this.activity.pinOverlay = mock(PinOverlay.class);

        this.activity.onPinColourChanged(PinColour.GREEN);

        verify(this.activity.mandelbrotFractalView).postUIThreadRedraw();
        verify(this.activity.pinOverlay).setPinColour(PinColour.GREEN);
    }

    @Test
    public void testOnMandelbrotColourSchemeChanged() {
        this.activity.mandelbrotStrategy = mock(IFractalComputeStrategy.class);

        this.activity.onMandelbrotColourSchemeChanged(EnumColourStrategy.PASTEL, false);

        verify(this.activity.mandelbrotStrategy).setColourStrategy(EnumColourStrategy.PASTEL);
    }

    @Test
    public void testOnJuliaColourSchemeChanged() {
        this.activity.juliaStrategy = mock(IFractalComputeStrategy.class);

        this.activity.onJuliaColourSchemeChanged(EnumColourStrategy.PASTEL, false);

        verify(this.activity.juliaStrategy).setColourStrategy(EnumColourStrategy.PASTEL);
    }

    @Test
    public void testOnFractalViewReady() {
        FractalSceneActivity spiedActivity = spy(this.activity);
        IFractalPresenter presenter = mock(IFractalPresenter.class);
        Mockito.doNothing().when(spiedActivity).scheduleRecomputeBasedOnPreferences(presenter, true);

        spiedActivity.onFractalViewReady(presenter);

        verify(spiedActivity).scheduleRecomputeBasedOnPreferences(presenter, true);
    }

    @Test
    public void testReloadSelf() {
        FractalSceneActivity activity = spy(this.activity);
        activity.firstFractalView = mock(FractalView.class);
        activity.secondFractalView = mock(FractalView.class);
        activity.mandelbrotStrategy = mock(IFractalComputeStrategy.class);
        activity.juliaStrategy = mock(IFractalComputeStrategy.class);

        activity.reloadSelf();

        verify(activity.firstFractalView).tearDown();
        verify(activity.secondFractalView).tearDown();
        verify(activity.mandelbrotStrategy).tearDown();
        verify(activity.juliaStrategy).tearDown();
        verify(activity).finish();
        verify(activity).startActivity(any(Intent.class));
    }

    @Test
    public void testOnApplyChangesClicked() {
        FractalSceneActivity activity = spy(this.activity);
        activity.mandelbrotFractalPresenter = mock(FractalPresenter.class);
        activity.juliaFractalPresenter = mock(FractalPresenter.class);

        Mockito.doNothing().when(activity).scheduleRecomputeBasedOnPreferences(any(IFractalPresenter.class), anyBoolean());

        activity.onApplyChangesClicked();

        verify(activity.mandelbrotFractalPresenter).setFractalDetail(anyDouble());
        verify(activity.juliaFractalPresenter).setFractalDetail(anyDouble());
        verify(activity).scheduleRecomputeBasedOnPreferences(activity.mandelbrotFractalPresenter, true);
        verify(activity).scheduleRecomputeBasedOnPreferences(activity.juliaFractalPresenter, true);
    }

    @Test
    public void testOnFractalLongClick() {
        FractalSceneActivity activity = spy(this.activity);

        activity.onFractalLongClick(activity.mandelbrotFractalPresenter, 80f, 85f);
        activity.onFractalLongClick(activity.juliaFractalPresenter, 80f, 85f);

        verify(activity).openContextMenu(activity.mandelbrotFractalView);
        verify(activity).openContextMenu(activity.juliaFractalView);
    }

    @Test
    public void testOnFractalRecomputeScheduled() {
        this.activity.mandelbrotFractalPresenter = mock(FractalPresenter.class);
        this.activity.pinOverlay = mock(PinOverlay.class);
        Mockito.doNothing().when(this.activity.pinOverlay).setPosition(anyFloat(), anyFloat());
        when(this.activity.mandelbrotFractalPresenter.getPointFromGraphPosition(anyDouble(), anyDouble())).thenReturn(new double[] {1.0, 1.0});

        this.activity.onFractalRecomputeScheduled(this.activity.mandelbrotFractalPresenter);

        verify(this.activity.mandelbrotFractalPresenter).getPointFromGraphPosition(anyDouble(), anyDouble());
        verify(this.activity.pinOverlay).setPosition(anyFloat(), anyFloat());
    }
}