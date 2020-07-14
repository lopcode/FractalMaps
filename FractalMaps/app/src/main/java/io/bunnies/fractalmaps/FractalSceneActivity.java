package io.bunnies.fractalmaps;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.bunnies.fractalmaps.colouring.EnumColourStrategy;
import io.bunnies.fractalmaps.compute.strategies.IFractalComputeStrategy;
import io.bunnies.fractalmaps.compute.strategies.JuliaSeedSettable;
import io.bunnies.fractalmaps.compute.strategies.renderscript.JuliaRenderscriptFractalComputeStrategy;
import io.bunnies.fractalmaps.compute.strategies.renderscript.MandelbrotRenderscriptFractalComputeStrategy;
import io.bunnies.fractalmaps.compute.strategies.renderscript.RenderscriptFractalComputeStrategy;
import io.bunnies.fractalmaps.detail.DetailControlDelegate;
import io.bunnies.fractalmaps.detail.DetailControlDialog;
import io.bunnies.fractalmaps.menu.IFractalMenuDelegate;
import io.bunnies.fractalmaps.menu.ISceneMenuDelegate;
import io.bunnies.fractalmaps.overlay.IFractalOverlay;
import io.bunnies.fractalmaps.overlay.pin.IPinMovementDelegate;
import io.bunnies.fractalmaps.overlay.pin.PinColour;
import io.bunnies.fractalmaps.overlay.pin.PinOverlay;
import io.bunnies.fractalmaps.presenter.FractalPresenter;
import io.bunnies.fractalmaps.presenter.IFractalPresenter;
import io.bunnies.fractalmaps.settings.FractalTypeEnum;
import io.bunnies.fractalmaps.settings.SceneLayoutEnum;
import io.bunnies.fractalmaps.settings.SettingsActivity;
import io.bunnies.fractalmaps.settings.SettingsManager;
import io.bunnies.fractalmaps.settings.saved_state.SavedGraphArea;
import io.bunnies.fractalmaps.settings.saved_state.SavedJuliaGraph;
import io.bunnies.fractalmaps.touch.FractalTouchHandler;
import io.bunnies.fractalmaps.touch.MandelbrotTouchHandler;
import io.bunnies.fractalmaps.view.FractalView;

public class FractalSceneActivity extends AppCompatActivity implements IFractalSceneDelegate, IPinMovementDelegate, DetailControlDelegate, ISceneMenuDelegate, IFractalMenuDelegate {
    private final Logger LOGGER = LoggerFactory.getLogger(FractalSceneActivity.class);

    // Layout variables
    Toolbar toolbar;
    TextView toolbarTextProgress;
    FractalView firstFractalView;
    FractalView secondFractalView;

    public static final String FRAGMENT_DETAIL_DIALOG_NAME = "detailControlDialog";
    private Map<IFractalPresenter, Boolean> UIRenderStates = new HashMap<>();
    private SceneLayoutEnum layoutType;

    public long sceneStartTime = 0;
    private static final int BUTTON_SPAM_MINIMUM_MS = 1000;

    // Views
    public FractalView mandelbrotFractalView;
    public FractalView juliaFractalView;

    // Presenters
    public FractalPresenter mandelbrotFractalPresenter;
    public FractalPresenter juliaFractalPresenter;

    // Strategies
    public IFractalComputeStrategy mandelbrotStrategy;
    public IFractalComputeStrategy juliaStrategy;
    public JuliaSeedSettable juliaSetter;

    // Overlays
    private List<IFractalOverlay> sceneOverlays;
    public PinOverlay pinOverlay;

    private float previousPinDragX = 0;
    private float previousPinDragY = 0;

    // Settings
    public SettingsManager settings;
    private boolean showingPinOverlay = true;

    // Context menus
    private float pinContextX = 0;
    private float pinContextY = 0;
    public View viewContext;
    private boolean contextFromTouchHandler = false;

    // Android lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        LOGGER.debug("OnCreate");

        this.sceneStartTime = System.nanoTime();

        this.settings = new SettingsManager(this);
        this.settings.setFractalSceneDelegate(this);

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(this.settings);

        this.layoutType = this.settings.getLayoutType();
        this.setContentViewFromLayoutType(this.layoutType);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        if (this.settings.isFirstTimeUse()) {
            showIntro();
        }

        this.showingPinOverlay = (this.layoutType == SceneLayoutEnum.SIDE_BY_SIDE || (this.layoutType == SceneLayoutEnum.LARGE_SMALL && !this.settings.getViewsSwitched()));

        toolbar = findViewById(R.id.toolbar);
        toolbarTextProgress = findViewById(R.id.toolbarTextProgress);
        firstFractalView = findViewById(R.id.firstFractalView);
        secondFractalView = findViewById(R.id.secondFractalView);

        this.firstFractalView.initialise();
        this.secondFractalView.initialise();

        this.initialiseToolbar();

        this.initialiseMandelbrotPresenter();
        this.initialiseJuliaPresenter();
        this.initialiseViews();

        this.initialiseOverlays();
        this.settings.refreshColourSettings();

        MandelbrotJuliaLocation savedParameters = this.loadSavedParameters(savedInstanceState);
        this.initialiseFractalParameters(savedParameters);

        this.initialiseRenderStates();

        if (this.settings.isFirstTimeUse()) {
            this.settings.setFirstTimeUse(false);
        }
    }

    private MandelbrotJuliaLocation loadSavedParameters(Bundle savedInstanceState) {
        double[] juliaParams = MandelbrotJuliaLocation.defaultJuliaParams.clone();
        double[] juliaGraphArea = MandelbrotJuliaLocation.defaultJuliaGraphArea.clone();
        double[] mainGraphArea = MandelbrotJuliaLocation.defaultMandelbrotGraphArea.clone();

        if (savedInstanceState != null) {
            LOGGER.debug("Restoring instance state");

            try {
                mainGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA);
                juliaGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA);
                juliaParams = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS);
            } catch (Exception e) {
                LOGGER.warn("Failed to restore instance state, using some defaults");
            }
        } else {
            LOGGER.debug("No saved instance state bundle, trying shared preferences");

            SavedGraphArea savedMandelbrotGraph = this.settings.getPreviousMandelbrotGraph();
            SavedJuliaGraph savedJuliaGraph = this.settings.getPreviousJuliaGraph();

            if (savedMandelbrotGraph != null) {
                LOGGER.debug("Restoring Mandelbrot from SharedPrefs");

                mainGraphArea[0] = savedMandelbrotGraph.graphX;
                mainGraphArea[1] = savedMandelbrotGraph.graphY;
                mainGraphArea[2] = savedMandelbrotGraph.graphZ;
            }

            if (savedJuliaGraph != null) {
                LOGGER.debug("Restoring Julia from SharedPrefs");

                juliaGraphArea[0] = savedJuliaGraph.graphX;
                juliaGraphArea[1] = savedJuliaGraph.graphY;
                juliaGraphArea[2] = savedJuliaGraph.graphZ;

                juliaParams[0] = savedJuliaGraph.juliaSeedX;
                juliaParams[1] = savedJuliaGraph.juliaSeedY;
            }
        }

        return new MandelbrotJuliaLocation(mainGraphArea, juliaGraphArea, juliaParams);
    }

    public void setContentViewFromLayoutType(SceneLayoutEnum layoutType) {
        switch (layoutType) {
            case SIDE_BY_SIDE:
                this.setContentView(io.bunnies.fractalmaps.R.layout.fractals_two_next);
                break;

            case LARGE_SMALL:
                this.setContentView(io.bunnies.fractalmaps.R.layout.fractals_large_small);
                break;

            default:
                this.setContentView(io.bunnies.fractalmaps.R.layout.fractals_two_next);
                break;
        }

        this.layoutType = layoutType;
    }

    private void initialiseToolbar() {
        setSupportActionBar(this.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
    }

    private void initialiseMandelbrotPresenter() {
        this.mandelbrotStrategy = new MandelbrotRenderscriptFractalComputeStrategy();
        ((RenderscriptFractalComputeStrategy) this.mandelbrotStrategy).setContext(this);

        this.mandelbrotFractalPresenter = new FractalPresenter(this, this, mandelbrotStrategy);

        if (this.showingPinOverlay) {
            MandelbrotTouchHandler mandelbrotTouchHandler = new MandelbrotTouchHandler(this, this.mandelbrotFractalPresenter);
            mandelbrotTouchHandler.setPinMovementDelegate(this);
            this.mandelbrotFractalPresenter.setTouchHandler(mandelbrotTouchHandler);
        } else {
            this.mandelbrotFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.mandelbrotFractalPresenter));
        }

        this.mandelbrotFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.shiftGraphAreaIfDefault(this.mandelbrotFractalPresenter);
    }

    public void initialiseJuliaPresenter() {
        this.juliaStrategy = new JuliaRenderscriptFractalComputeStrategy();
        this.juliaSetter = (JuliaSeedSettable) this.juliaStrategy;
        ((RenderscriptFractalComputeStrategy) this.juliaStrategy).setContext(this);

        this.juliaFractalPresenter = new FractalPresenter(this, this, juliaStrategy);
        this.juliaFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.juliaFractalPresenter));

        this.juliaFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.shiftGraphAreaIfDefault(this.juliaFractalPresenter);
    }

    public void initialiseViews() {
        if (!this.settings.getViewsSwitched()) {
            this.mandelbrotFractalView = this.firstFractalView;
            this.juliaFractalView = this.secondFractalView;

            this.firstFractalView.setResizeListener(this.mandelbrotFractalPresenter);
            this.secondFractalView.setResizeListener(this.juliaFractalPresenter);

            this.mandelbrotFractalPresenter.setView(this.firstFractalView, new Matrix(), this.mandelbrotFractalPresenter);
            this.juliaFractalPresenter.setView(this.secondFractalView, new Matrix(), this.juliaFractalPresenter);
        } else {
            this.mandelbrotFractalView = this.secondFractalView;
            this.juliaFractalView = this.firstFractalView;

            this.firstFractalView.setResizeListener(this.juliaFractalPresenter);
            this.secondFractalView.setResizeListener(this.mandelbrotFractalPresenter);

            this.mandelbrotFractalPresenter.setView(this.secondFractalView, new Matrix(), this.mandelbrotFractalPresenter);
            this.juliaFractalPresenter.setView(this.firstFractalView, new Matrix(), this.juliaFractalPresenter);
        }

        this.registerForContextMenu(this.mandelbrotFractalView);
        this.registerForContextMenu(this.juliaFractalView);
    }

    public void initialiseFractalParameters(MandelbrotJuliaLocation parameters) {
        this.mandelbrotFractalPresenter.setGraphArea(parameters.getMandelbrotGraphArea());
        this.juliaFractalPresenter.setGraphArea(parameters.getJuliaGraphArea());
        ((JuliaSeedSettable) this.juliaStrategy).setJuliaSeed(parameters.getJuliaParam()[0], parameters.getJuliaParam()[1]);
    }

    public void initialiseRenderStates() {
        this.UIRenderStates.put(this.mandelbrotFractalPresenter, false);
        this.UIRenderStates.put(this.juliaFractalPresenter, false);
    }

    public void initialiseOverlays() {
        this.sceneOverlays = new ArrayList<>();

        if (this.showingPinOverlay) {
            this.pinOverlay = new PinOverlay(this, 42.0f, 100f, 100f);
            this.settings.refreshPinSettings();
            this.sceneOverlays.add(this.pinOverlay);
        }

        this.mandelbrotFractalPresenter.onSceneOverlaysChanged(this.sceneOverlays);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.saveGraphStates();

        this.mandelbrotStrategy.tearDown();
        this.juliaStrategy.tearDown();

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this.settings);
    }

    public void saveGraphStates() {
        double[] mandelbrotGraphArea = this.mandelbrotFractalPresenter.getGraphArea();
        double[] juliaGraphArea = this.juliaFractalPresenter.getGraphArea();
        double[] juliaParams = this.juliaSetter.getJuliaSeed();

        this.settings.savePreviousMandelbrotGraph(new SavedGraphArea(mandelbrotGraphArea[0], mandelbrotGraphArea[1], mandelbrotGraphArea[2]));
        this.settings.savePreviousJuliaGraph(new SavedJuliaGraph(juliaGraphArea[0], juliaGraphArea[1], juliaGraphArea[2], juliaParams[0], juliaParams[1]));
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.saveGraphStates();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this.settings);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        LOGGER.debug("Saving instance state");

        outState.putDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA, this.mandelbrotFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA, this.juliaFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS, this.juliaSetter.getJuliaSeed());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        LOGGER.debug("Restoring instance state");

        double[] mainGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA);
        double[] juliaGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA);
        double[] juliaParams = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS);

        this.juliaSetter.setJuliaSeed(juliaParams[0], juliaParams[1]);
        this.mandelbrotFractalPresenter.setGraphArea(mainGraphArea);
        this.juliaFractalPresenter.setGraphArea(juliaGraphArea);
    }

    // Menu creation and handling

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(io.bunnies.fractalmaps.R.menu.global_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long timeDiffInMS = (System.nanoTime() - this.sceneStartTime) / 1000000;
        LOGGER.debug("Time (ms) since start of scene {}", timeDiffInMS);

        switch (item.getItemId()) {
            case io.bunnies.fractalmaps.R.id.menuResetAll:
                this.onResetAllClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuHelp:
                this.onHelpClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuShowMandelbrotMenu:
                this.onShowMandelbrotMenuClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuShowJuliaMenu:
                this.onShowJuliaMenuClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuSettings:
                this.onSettingsClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkOld:
                this.onBenchmarkOldClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkOne:
                this.onBenchmarkOneClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkTwo:
                this.onBenchmarkTwoClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkThree:
                this.onBenchmarkThreeClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkFour:
                this.onBenchmarkFourClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkFive:
                this.onBenchmarkFiveClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkSix:
                this.onBenchmarkSixClicked();
                return true;

            case io.bunnies.fractalmaps.R.id.menuBenchmarkSeven:
                this.onBenchmarkSevenClicked();
                return true;

            default:
                return false;
        }
    }

    // Context menus

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (!this.contextFromTouchHandler)
            return;

        super.onCreateContextMenu(menu, v, menuInfo);

        this.viewContext = v;

        if (v == this.mandelbrotFractalView) {
            LOGGER.debug("Inflating Mandelbrot context menu");

            this.inflateMandelbrotContextMenu(menu);
        } else if (v == this.juliaFractalView) {
            LOGGER.debug("Inflating Julia context menu");

            this.inflateJuliaContextMenu(menu);
        }

        this.contextFromTouchHandler = false;
    }

    public void inflateMandelbrotContextMenu(ContextMenu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(io.bunnies.fractalmaps.R.menu.fractal_menu, menu);
        MenuItem placePinItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuPlacePin);
        if (placePinItem != null) {
            placePinItem.setVisible(true);
            placePinItem.setEnabled(this.showingPinOverlay);
        }

        menu.setHeaderTitle("Mandelbrot fractal");

        boolean viewsSwitched = this.settings.getViewsSwitched();
        boolean stillRendering = this.UIRenderStates.get(this.mandelbrotFractalPresenter);

        MenuItem resetItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuResetFractal);
        MenuItem changeLayoutItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuChangeLayout);
        MenuItem viewPositionItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuSwapViews);
        MenuItem saveItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuSave);
        MenuItem shareItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuShare);

        saveItem.setEnabled(!stillRendering);
        shareItem.setEnabled(!stillRendering);

        resetItem.setTitle(io.bunnies.fractalmaps.R.string.context_reset);

        if (this.layoutType == SceneLayoutEnum.SIDE_BY_SIDE) {
            changeLayoutItem.setTitle(io.bunnies.fractalmaps.R.string.context_make_big);

            boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);

            String movePosition;

            if (portrait) {
                movePosition = viewsSwitched ? "up" : "down";
            } else {
                movePosition = viewsSwitched ? "left" : "right";
            }

            viewPositionItem.setTitle(String.format(this.getResources().getString(io.bunnies.fractalmaps.R.string.context_change_view_position), movePosition));
        } else if (this.layoutType == SceneLayoutEnum.LARGE_SMALL) {
            changeLayoutItem.setTitle(io.bunnies.fractalmaps.R.string.context_view_side_side);

            String movePosition = viewsSwitched ? "down" : "up";

            viewPositionItem.setTitle(String.format(this.getResources().getString(io.bunnies.fractalmaps.R.string.context_change_view_position), movePosition));
        }
    }

    public void inflateJuliaContextMenu(ContextMenu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(io.bunnies.fractalmaps.R.menu.fractal_menu, menu);
        MenuItem placePinItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuPlacePin);
        if (placePinItem != null)
            placePinItem.setVisible(false);

        menu.setHeaderTitle("Julia fractal");

        boolean viewsSwitched = this.settings.getViewsSwitched();
        boolean stillRendering = this.UIRenderStates.get(this.juliaFractalPresenter);

        MenuItem resetItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuResetFractal);
        MenuItem changeLayoutItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuChangeLayout);
        MenuItem viewPositionItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuSwapViews);
        MenuItem saveItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuSave);
        MenuItem shareItem = menu.findItem(io.bunnies.fractalmaps.R.id.menuShare);

        saveItem.setEnabled(!stillRendering);
        shareItem.setEnabled(!stillRendering);

        resetItem.setTitle(io.bunnies.fractalmaps.R.string.context_reset);

        if (this.layoutType == SceneLayoutEnum.SIDE_BY_SIDE) {
            changeLayoutItem.setTitle(io.bunnies.fractalmaps.R.string.context_make_big);

            boolean portrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);

            String movePosition;

            if (portrait) {
                movePosition = viewsSwitched ? "down" : "up";
            } else {
                movePosition = viewsSwitched ? "right" : "left";
            }

            viewPositionItem.setTitle(String.format(this.getResources().getString(io.bunnies.fractalmaps.R.string.context_change_view_position), movePosition));
        } else if (this.layoutType == SceneLayoutEnum.LARGE_SMALL) {
            changeLayoutItem.setTitle(io.bunnies.fractalmaps.R.string.context_view_side_side);

            String movePosition = viewsSwitched ? "up" : "down";

            viewPositionItem.setTitle(String.format(this.getResources().getString(io.bunnies.fractalmaps.R.string.context_change_view_position), movePosition));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean handled = false;

        if (this.viewContext == this.mandelbrotFractalView) {
            handled = this.onMandelbrotContextItemSelected(item);
        } else if (this.viewContext == this.juliaFractalView) {
            handled = this.onJuliaContextItemSelected(item);
        }

        if (handled)
            return true;

        switch (item.getItemId()) {
            case io.bunnies.fractalmaps.R.id.menuSwapViews:
                this.onChangeFractalPositionClicked(this.viewContext);
                return true;

            case io.bunnies.fractalmaps.R.id.menuChangeLayout:
                this.onSwitchFractalViewLayoutClicked(this.viewContext);
                return true;

            case io.bunnies.fractalmaps.R.id.menuSave:
                this.saveImage(this.viewContext);
                return true;

            case io.bunnies.fractalmaps.R.id.menuShare:
                this.shareImage(this.viewContext);
                return true;

            default:
                LOGGER.debug("Context item selected that wasn't handled");
                return true;
        }
    }

    public boolean onMandelbrotContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case io.bunnies.fractalmaps.R.id.menuPlacePin:
                this.setPinPosition(this.pinContextX, this.pinContextY);
                return true;

            case io.bunnies.fractalmaps.R.id.menuResetFractal:
                this.onResetFractalClicked(this.mandelbrotFractalView);
                return true;

            default:
                return false;
        }
    }

    public boolean onJuliaContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case io.bunnies.fractalmaps.R.id.menuResetFractal:
                this.onResetFractalClicked(this.juliaFractalView);
                return true;

            default:
                return false;
        }
    }

    // Scene Menu Delegate

    @Override
    public void onResetAllClicked() {
        this.resetMandelbrotFractal();
        this.resetJuliaFractal();
    }

    @Override
    public void onShowMandelbrotMenuClicked() {
        float pinX = 0;
        float pinY = 0;

        if (this.showingPinOverlay) {
            pinX = this.getPinX();
            pinY = this.getPinY();
        }

        this.onFractalLongClick(this.mandelbrotFractalPresenter, pinX, pinY);
    }

    @Override
    public void onShowJuliaMenuClicked() {
        this.onFractalLongClick(this.juliaFractalPresenter, 0, 0);
    }

    public void onHelpClicked() {
        this.showHelpDialog();
    }

    public void onSettingsClicked() {
        this.startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onDetailClicked() {
        this.showDetailDialog();
    }

    public void onBenchmarkOldClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.631509065569354;
        benchmarkPoint[1] = 0.0008548063308817164;
        benchmarkPoint[2] = 0.0027763525271276013;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkOneClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -3.1;
        benchmarkPoint[1] = 1.5625;
        benchmarkPoint[2] = 5.0;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkTwoClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.7906918092188577;
        benchmarkPoint[1] = 0.015713398761235824;
        benchmarkPoint[2] = 0.054304181944388796;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkThreeClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.7866528244733257;
        benchmarkPoint[1] = 3.767225355612155E-4;
        benchmarkPoint[2] = 0.001246485714778256;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkFourClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.7864416057489034;
        benchmarkPoint[1] = 5.070184209076404E-6;
        benchmarkPoint[2] = 1.6176061854888957E-5;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkFiveClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.7864403263654793;
        benchmarkPoint[1] = 1.1249847143000328E-7;
        benchmarkPoint[2] = 3.449179104553224E-7;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkSixClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.7864402559061188;
        benchmarkPoint[1] = 1.7764552729039504E-9;
        benchmarkPoint[2] = 6.143618724863131E-9;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    public void onBenchmarkSevenClicked() {
        double[] benchmarkPoint = new double[3];

        benchmarkPoint[0] = -1.786440255616136;
        benchmarkPoint[1] = 4.880132782623177E-11;
        benchmarkPoint[2] = 1.6752488285476375E-10;

        this.mandelbrotFractalPresenter.computeGraphAreaNow(benchmarkPoint);
    }

    // Context Specific Menus

    @Override
    public void onResetFractalClicked(View viewContext) {
        if (viewContext == this.mandelbrotFractalView) {
            this.resetMandelbrotFractal();
        } else {
            this.resetJuliaFractal();
        }
    }

    @Override
    public void onSwitchFractalViewLayoutClicked(View viewContext) {
        long timeDiffInMS = (System.nanoTime() - this.sceneStartTime) / 1000000;
        LOGGER.debug("Time (ms) since start of scene " + timeDiffInMS);

        boolean viewsSwitched = this.settings.getViewsSwitched();
        if (timeDiffInMS > BUTTON_SPAM_MINIMUM_MS) {
            if (this.layoutType == SceneLayoutEnum.SIDE_BY_SIDE) {
                if (viewContext == this.mandelbrotFractalView) {
                    if (viewsSwitched)
                        this.settings.setViewsSwitched(false);
                } else if (viewContext == this.juliaFractalView) {
                    if (!viewsSwitched)
                        this.settings.setViewsSwitched(true);
                }
            }

            this.toggleLayoutType();
        }
    }

    @Override
    public void onChangeFractalPositionClicked(View viewContext) {
        long timeDiffInMS = (System.nanoTime() - this.sceneStartTime) / 1000000;
        LOGGER.debug("Time (ms) since start of scene {}", timeDiffInMS);

        if (timeDiffInMS > BUTTON_SPAM_MINIMUM_MS) {
            this.settings.setViewsSwitched(!this.settings.getViewsSwitched());

            this.reloadSelf();
        }
    }

    @Override
    public void onSaveFractalClicked(View viewContext) {
        this.saveImage(viewContext);
    }

    @Override
    public void onShareFractalClicked(View viewContext) {
        this.shareImage(viewContext);
    }

    public void resetMandelbrotFractal() {
        this.mandelbrotFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultMandelbrotGraphArea);
        this.onFractalViewReady(this.mandelbrotFractalPresenter);
    }

    public void resetJuliaFractal() {
        this.juliaFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultJuliaGraphArea);
        this.juliaSetter.setJuliaSeed(MandelbrotJuliaLocation.defaultJuliaParams[0], MandelbrotJuliaLocation.defaultJuliaParams[1]);

        double[] pinPosition = this.mandelbrotFractalPresenter.getPointFromGraphPosition(MandelbrotJuliaLocation.defaultJuliaParams[0], MandelbrotJuliaLocation.defaultJuliaParams[1]);
        this.setPinPosition((float) pinPosition[0], (float) pinPosition[1]);
        this.onFractalViewReady(this.juliaFractalPresenter);
    }

    public void toggleLayoutType() {
        if (this.layoutType == SceneLayoutEnum.LARGE_SMALL) {
            this.settings.setLayoutType(SceneLayoutEnum.SIDE_BY_SIDE);
        } else {
            this.settings.setLayoutType(SceneLayoutEnum.LARGE_SMALL);
        }
    }

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter, boolean fullRefresh) {
        presenter.getComputeStrategy().stopAllRendering();

        if (fullRefresh)
            presenter.clearPixelSizes();

        if (settings.performCrudeFirst()) {
            presenter.recomputeGraph(FractalPresenter.CRUDE_PIXEL_BLOCK);
        }

        presenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }

    @Override
    public void onPinColourChanged(PinColour colour) {
        this.pinOverlay.setPinColour(colour);
        this.mandelbrotFractalView.postUIThreadRedraw();
    }

    @Override
    public void onMandelbrotColourSchemeChanged(EnumColourStrategy colourStrategy, boolean reRender) {
        this.mandelbrotStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.mandelbrotFractalPresenter, true);
    }

    @Override
    public void onJuliaColourSchemeChanged(EnumColourStrategy colourStrategy, boolean reRender) {
        this.juliaStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.juliaFractalPresenter, true);
    }

    @Override
    public void onFractalViewReady(IFractalPresenter presenter) {
        LOGGER.debug("Fractal view ready");

        this.shiftGraphAreaIfDefault(presenter);
        this.scheduleRecomputeBasedOnPreferences(presenter, true);
    }

    private void shiftGraphAreaIfDefault(IFractalPresenter presenter) {
        // Move the fractal down a to the mid point of the view
        //  Only if the graph area is the default, otherwise it got set manually
        View view = null;
        boolean isDefaultGraphArea = false;
        if (presenter == this.mandelbrotFractalPresenter) {
            view = this.firstFractalView;
            isDefaultGraphArea = this.mandelbrotFractalPresenter.getGraphArea() == MandelbrotJuliaLocation.defaultMandelbrotGraphArea;
        } else if (presenter == this.juliaFractalPresenter) {
            view = this.secondFractalView;
            isDefaultGraphArea = this.juliaFractalPresenter.getGraphArea() == MandelbrotJuliaLocation.defaultJuliaGraphArea;
        }

        if (isDefaultGraphArea) {
            LOGGER.debug("Moved default graph area to midpoint of view");
            double[] graphMidPoint = presenter.getGraphPositionFromClickedPosition(view.getWidth() / 2.0f, view.getHeight() / 2.0f);
            double[] originalGraphPoint = presenter.getGraphArea();
            originalGraphPoint[1] -= graphMidPoint[1];
            presenter.setGraphArea(originalGraphPoint);
        }
    }

    @Override
    public void onSceneLayoutChanged(SceneLayoutEnum layoutType) {
        this.reloadSelf();
    }

    public void reloadSelf() {
        this.mandelbrotStrategy.tearDown();
        this.juliaStrategy.tearDown();
        this.firstFractalView.tearDown();
        this.secondFractalView.tearDown();

        this.finish();
        this.startActivity(this.getIntent());
    }

    // Image saving and sharing

    private void saveImage(View viewContext) {
        if (viewContext == this.juliaFractalView) {
            LOGGER.info("Saving Julia image");

            this.saveJuliaImage();
        } else if (viewContext == this.mandelbrotFractalView) {
            LOGGER.info("Saving Mandelbrot image");

            this.saveMandelbrotImage();
        }
    }

    private void saveMandelbrotImage() {
        Bitmap bitmap = this.mandelbrotFractalView.getCurrentBitmap();
        this.saveImage(bitmap, this.formImageTitle("Mandelbrot"));
    }

    private void saveJuliaImage() {
        Bitmap bitmap = this.juliaFractalView.getCurrentBitmap();
        this.saveImage(bitmap, this.formImageTitle("Julia"));
    }

    private File saveImage(Bitmap bitmap, String title) {
        File fractalImage = this.getImageOutputFile(title);
        LOGGER.info("Saving '{}' to '{}'", title, fractalImage.getAbsolutePath());

        OutputStream output = null;
        try {
            output = new FileOutputStream(fractalImage);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to open file for writing: {}", e);
        }

        boolean couldWriteImage = this.writeBitmapToPNGStream(output, bitmap);
        if (!couldWriteImage) {
            this.showLongToast("Failed to save image!");
            return null;
        }

        addImageToGallery(fractalImage.getAbsolutePath(), this.getApplicationContext());
        this.showShortToast("Saved " + title + " to " + fractalImage.getAbsolutePath());

        return fractalImage;
    }

    private File getImageOutputFile(String title) {
        File path = Environment.getExternalStorageDirectory();
        File fractalsDirectory = new File(path.getAbsolutePath() + "/FractalMaps/");
        File fractalImage = new File(fractalsDirectory, title + ".png");

        this.createApplicationDirectoryIfNecessary();
        return fractalImage;
    }

    private boolean writeBitmapToPNGStream(OutputStream output, Bitmap bitmap) {
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            output.flush();
            output.close();
        } catch (Exception e) {
            LOGGER.error("Failed to write image: {}", e);

            return false;
        }

        return true;
    }

    private void createApplicationDirectoryIfNecessary() {
        File path = Environment.getExternalStorageDirectory();
        File fractalsDirectory = new File(path.getAbsolutePath() + "/FractalMaps/");

        if (!fractalsDirectory.exists()) {
            LOGGER.info("Fractal images directory doesn't exist, creating it: {}", fractalsDirectory.getAbsolutePath());
            fractalsDirectory.mkdirs();
        }
    }

    private String formImageTitle(String fractalType) {
        return fractalType + (System.currentTimeMillis() / 1000L);
    }

    // http://stackoverflow.com/questions/20859584/how-save-image-in-android-gallery
    public static void addImageToGallery(final String filePath, final Context context) {
        ContentValues values = new ContentValues();

        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.DATA, filePath);

        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void shareImage(View viewContext) {
        if (viewContext == this.juliaFractalView) {
            LOGGER.info("Saving Julia image");

            this.shareJuliaImage();
        } else if (viewContext == this.mandelbrotFractalView) {
            LOGGER.info("Saving Mandelbrot image");

            this.shareMandelbrotImage();
        }
    }

    private void shareMandelbrotImage() {
        Bitmap bitmap = this.mandelbrotFractalView.getCurrentBitmap();
        this.shareImage(bitmap, this.formImageTitle("Mandelbrot"));
    }

    private void shareJuliaImage() {
        Bitmap bitmap = this.juliaFractalView.getCurrentBitmap();
        this.shareImage(bitmap, this.formImageTitle("Julia"));
    }

    private boolean shareImage(Bitmap bitmap, String title) {
        File image = this.saveImage(bitmap, title);
        if (image == null) {
            LOGGER.error("Couldn't share image because it couldn't be saved!");
            this.showShortToast("Failed to share image, because it couldn't be saved first");

            return false;
        }

        Uri uri = Uri.fromFile(image);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/png");

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "Share " + title + " image using"));

        return true;
    }

    // Utilities

    public void showLongToast(final String toastText) {
        this.showToast(toastText, Toast.LENGTH_LONG);
    }

    public void showShortToast(final String toastText) {
        this.showToast(toastText, Toast.LENGTH_SHORT);
    }

    private void showToast(final String toastText, final int timeLength) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), toastText, timeLength).show();
            }
        });
    }

    /* Show the short tutorial/intro dialog */
    private void showIntro() {
        TextView text = new TextView(this);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(Html.fromHtml(getString(io.bunnies.fractalmaps.R.string.intro_text)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setView(text)
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.create().show();
    }

    /* Show the large help dialog */
    private void showHelpDialog() {
        ScrollView scrollView = new ScrollView(this);
        TextView text = new TextView(this);
        text.setText(Html.fromHtml(getString(io.bunnies.fractalmaps.R.string.help_text)));
        scrollView.addView(text);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setView(scrollView)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog helpDialog = builder.create();
        helpDialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        helpDialog.show();
    }

    // Dialogs

    private void showDetailDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DetailControlDialog detailControlDialog = new DetailControlDialog();
        detailControlDialog.show(fm, FRAGMENT_DETAIL_DIALOG_NAME);
    }

    private void dismissDetailDialog() {
        Fragment dialog = getSupportFragmentManager().findFragmentByTag(FRAGMENT_DETAIL_DIALOG_NAME);
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismiss();
        }
    }

    // Detail control delegate

    @Override
    public void onApplyChangesClicked() {
        this.dismissDetailDialog();

        this.mandelbrotFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.juliaFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.scheduleRecomputeBasedOnPreferences(this.mandelbrotFractalPresenter, true);
        this.scheduleRecomputeBasedOnPreferences(this.juliaFractalPresenter, true);
    }

    @Override
    public void onCancelClicked() {
        this.dismissDetailDialog();
    }

    // IFractalSceneDelegate

    @Override
    public void setRenderingStatus(IFractalPresenter presenter, boolean rendering) {
        this.UIRenderStates.put(presenter, rendering);

        boolean atLeastOnePresenterRendering = false;
        Iterator iterator = this.UIRenderStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry presenterRendering = (Map.Entry) iterator.next();
            if ((Boolean) presenterRendering.getValue())
                atLeastOnePresenterRendering = true;
        }

        if (atLeastOnePresenterRendering) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbarTextProgress.setVisibility(View.VISIBLE);
                }
            });
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbarTextProgress.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onFractalLongClick(IFractalPresenter presenter, float x, float y) {
        this.contextFromTouchHandler = true;

        if (presenter == this.mandelbrotFractalPresenter) {
            this.openContextMenu(this.mandelbrotFractalView);

            if (this.showingPinOverlay) {
                this.pinContextX = x;
                this.pinContextY = y;
            }
        } else {
            this.openContextMenu(this.juliaFractalView);
        }
    }

    private void setPinPosition(float x, float y) {
        this.pinOverlay.setPosition(x, y);
        this.mandelbrotFractalView.postUIThreadRedraw();

        double[] graphTapPosition = this.mandelbrotFractalPresenter.getGraphPositionFromClickedPosition(x, y);
        this.setJuliaSeedAndRecompute(graphTapPosition, FractalPresenter.DEFAULT_PIXEL_SIZE);
    }

    private void setJuliaSeedAndRecompute(double[] juliaSeed, int pixelBlockSize) {
        this.juliaSetter.setJuliaSeed(juliaSeed[0], juliaSeed[1]);

        this.juliaFractalPresenter.clearPixelSizes();
        this.juliaFractalPresenter.recomputeGraph(pixelBlockSize);
    }

    @Override
    public void onFractalRecomputeScheduled(IFractalPresenter presenter) {
        if (presenter != this.mandelbrotFractalPresenter)
            return;

        double[] juliaSeed = this.juliaSetter.getJuliaSeed();
        double[] newPinPoint = this.mandelbrotFractalPresenter.getPointFromGraphPosition(juliaSeed[0], juliaSeed[1]);

        if (this.showingPinOverlay) {
            this.pinOverlay.setPosition((float) newPinPoint[0], (float) newPinPoint[1]);
        }
    }

    @Override
    public void onFractalRecomputed(IFractalPresenter presenter, double timeTakenInSeconds) {
        if (this.settings.showTimes()) {
            String toastText = "Render took " + timeTakenInSeconds + " seconds";
            this.showShortToast(toastText);
        }
    }

    // IPinMovementDelegate

    @Override
    public void pinDragged(float x, float y, boolean forceUpdate) {
        this.pinOverlay.setPosition(x, y);

        this.mandelbrotFractalView.postUIThreadRedraw();

        double dragDistance = Math.sqrt(Math.pow(this.previousPinDragX - x, 2) + Math.pow(this.previousPinDragY - y, 2));
        if (dragDistance > 1 || forceUpdate) {
            double[] graphTapPosition = this.mandelbrotFractalPresenter.getGraphPositionFromClickedPosition(x, y);
            this.setJuliaSeedAndRecompute(graphTapPosition, FractalPresenter.CRUDE_PIXEL_BLOCK);

            this.previousPinDragX = x;
            this.previousPinDragY = y;
        }
    }

    @Override
    public float getPinX() {
        return this.pinOverlay.getX();
    }

    @Override
    public float getPinY() {
        return this.pinOverlay.getY();
    }

    @Override
    public float getPinRadius() {
        return this.pinOverlay.getPinRadius();
    }

    @Override
    public void startedDraggingPin() {
        this.pinOverlay.setHilighted(true);
    }

    @Override
    public void stoppedDraggingPin(float x, float y) {
        this.pinDragged(x, y, true);
        this.pinOverlay.setHilighted(false);
        this.juliaStrategy.stopAllRendering();
        this.juliaFractalPresenter.clearPixelSizes();
        this.juliaFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }
}
