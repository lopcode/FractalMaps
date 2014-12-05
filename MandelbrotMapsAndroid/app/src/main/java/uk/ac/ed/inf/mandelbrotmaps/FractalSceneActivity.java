package uk.ac.ed.inf.mandelbrotmaps;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;
import uk.ac.ed.inf.mandelbrotmaps.colouring.IColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.IFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.JuliaSeedSettable;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.cpu.JuliaCPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.cpu.MandelbrotCPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu.GPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu.JuliaGPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu.MandelbrotGPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDelegate;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDialog;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuClickDelegate;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuDialog;
import uk.ac.ed.inf.mandelbrotmaps.overlay.IFractalOverlay;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.IPinMovementDelegate;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinColour;
import uk.ac.ed.inf.mandelbrotmaps.overlay.pin.PinOverlay;
import uk.ac.ed.inf.mandelbrotmaps.presenter.FractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.presenter.IFractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.settings.FractalTypeEnum;
import uk.ac.ed.inf.mandelbrotmaps.settings.SceneLayoutEnum;
import uk.ac.ed.inf.mandelbrotmaps.settings.SettingsActivity;
import uk.ac.ed.inf.mandelbrotmaps.settings.SettingsManager;
import uk.ac.ed.inf.mandelbrotmaps.settings.saved_state.SavedGraphArea;
import uk.ac.ed.inf.mandelbrotmaps.settings.saved_state.SavedJuliaGraph;
import uk.ac.ed.inf.mandelbrotmaps.touch.FractalTouchHandler;
import uk.ac.ed.inf.mandelbrotmaps.touch.MandelbrotTouchHandler;
import uk.ac.ed.inf.mandelbrotmaps.view.FractalView;

public class FractalSceneActivity extends ActionBarActivity implements IFractalSceneDelegate, IPinMovementDelegate, MenuClickDelegate, DetailControlDelegate {
    // Layout variables
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    //@InjectView(R.id.toolbarProgress)
    //ProgressBar toolbarProgress;

    @InjectView(R.id.firstFractalView)
    FractalView firstFractalView;

    @InjectView(R.id.secondFractalView)
    FractalView secondFractalView;

    FractalView mandelbrotFractalView;
    FractalView juliaFractalView;

    private FractalPresenter mandelbrotFractalPresenter;
    private FractalPresenter juliaFractalPresenter;

    private IFractalComputeStrategy mandelbrotStrategy;
    private IFractalComputeStrategy juliaStrategy;

    // File saving variables
    private ProgressDialog savingDialog;
    private File imagefile;
    private Boolean cancelledSave = false;

    public static final String FRAGMENT_MENU_DIALOG_NAME = "menuDialog";
    public static final String FRAGMENT_DETAIL_DIALOG_NAME = "detailControlDialog";

    private HashMap<IFractalPresenter, Boolean> UIRenderStates = new HashMap<IFractalPresenter, Boolean>();

    private SceneLayoutEnum layoutType;

    // Overlays
    private List<IFractalOverlay> sceneOverlays;
    private PinOverlay pinOverlay;

    private float previousPinDragX = 0;
    private float previousPinDragY = 0;

    private SettingsManager settings;

    private static boolean shouldGPURender = true;
    private boolean showingPinOverlay = true;

    private long sceneStartTime = 0;

    // Android lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        Log.i("FA", "OnCreate");

        this.sceneStartTime = System.nanoTime();

        this.settings = new SettingsManager(this);
        this.settings.setFractalSceneDelegate(this);

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(this.settings);

        this.layoutType = this.settings.getLayoutType();
        this.setContentViewFromLayoutType(this.layoutType);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        if (this.settings.isFirstTimeUse()) {
            showIntro();
        }

        this.showingPinOverlay = (this.layoutType == SceneLayoutEnum.SIDE_BY_SIDE || (this.layoutType == SceneLayoutEnum.LARGE_SMALL && !this.settings.getViewsSwitched()));

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
    }

    private MandelbrotJuliaLocation loadSavedParameters(Bundle savedInstanceState) {
        double[] juliaParams = MandelbrotJuliaLocation.defaultJuliaParams.clone();
        double[] juliaGraphArea = MandelbrotJuliaLocation.defaultJuliaGraphArea.clone();
        double[] mainGraphArea = MandelbrotJuliaLocation.defaultMandelbrotGraphArea.clone();

        if (savedInstanceState != null) {
            Log.i("FA", "Restoring instance state");

            try {
                mainGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA);
                juliaGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA);
                juliaParams = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS);
            } catch (Exception e) {
                Log.i("FA", "Failed to restore instance state, using some defaults");
            }
        } else {
            Log.i("FA", "No saved instance state bundle, trying shared preferences");

            SavedGraphArea savedMandelbrotGraph = this.settings.getPreviousMandelbrotGraph();
            SavedJuliaGraph savedJuliaGraph = this.settings.getPreviousJuliaGraph();

            if (savedMandelbrotGraph != null) {
                Log.i("FA", "Restoring Mandelbrot from SharedPrefs");

                mainGraphArea[0] = savedMandelbrotGraph.graphX;
                mainGraphArea[1] = savedMandelbrotGraph.graphY;
                mainGraphArea[2] = savedMandelbrotGraph.graphZ;
            }

            if (savedJuliaGraph != null) {
                Log.i("FA", "Restoring Julia from SharedPrefs");

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
                this.setContentView(R.layout.fractals_two_next);
                break;

            case LARGE_SMALL:
                this.setContentView(R.layout.fractals_large_small);
                break;

            default:
                this.setContentView(R.layout.fractals_two_next);
                break;
        }

        this.layoutType = layoutType;
        ButterKnife.inject(this);
    }

    public void initialiseToolbar() {
        setSupportActionBar(this.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
    }

    public void initialiseMandelbrotPresenter() {
        if (shouldGPURender) {
            this.mandelbrotStrategy = new MandelbrotGPUFractalComputeStrategy();
            ((GPUFractalComputeStrategy) this.mandelbrotStrategy).setContext(this);
        } else {
            this.mandelbrotStrategy = new MandelbrotCPUFractalComputeStrategy();
        }

        this.mandelbrotFractalPresenter = new FractalPresenter(this, this, mandelbrotStrategy);

        if (this.showingPinOverlay) {
            MandelbrotTouchHandler mandelbrotTouchHandler = new MandelbrotTouchHandler(this, this.mandelbrotFractalPresenter);
            mandelbrotTouchHandler.setPinMovementDelegate(this);
            this.mandelbrotFractalPresenter.setTouchHandler(mandelbrotTouchHandler);
        } else {
            this.mandelbrotFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.mandelbrotFractalPresenter));
        }
//        this.mandelbrotFractalPresenter.fractalStrategy.setColourStrategy(new DefaultColourStrategy());
        this.mandelbrotFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
    }

    public void initialiseJuliaPresenter() {
        if (shouldGPURender) {
            this.juliaStrategy = new JuliaGPUFractalComputeStrategy();
            ((GPUFractalComputeStrategy) this.juliaStrategy).setContext(this);
        } else {
            this.juliaStrategy = new JuliaCPUFractalComputeStrategy();
        }

        this.juliaFractalPresenter = new FractalPresenter(this, this, juliaStrategy);
        this.juliaFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.juliaFractalPresenter));
//        this.juliaFractalPresenter.fractalStrategy.setColourStrategy(new JuliaColourStrategy());
        this.juliaFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.JULIA));
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
        this.sceneOverlays = new ArrayList<IFractalOverlay>();

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

        this.mandelbrotFractalPresenter.fractalStrategy.tearDown();
        this.juliaFractalPresenter.fractalStrategy.tearDown();

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this.settings);
    }

    public void saveGraphStates() {
        double[] mandelbrotGraphArea = this.mandelbrotFractalPresenter.getGraphArea();
        double[] juliaGraphArea = this.juliaFractalPresenter.getGraphArea();
        double[] juliaParams = ((JuliaSeedSettable) this.juliaStrategy).getJuliaSeed();

        this.settings.savePreviousMandelbrotGraph(new SavedGraphArea(mandelbrotGraphArea[0], mandelbrotGraphArea[1], mandelbrotGraphArea[2]));
        this.settings.savePreviousJuliaGraph(new SavedJuliaGraph(juliaGraphArea[0], juliaGraphArea[1], juliaGraphArea[2], juliaParams[0], juliaParams[1]));
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.saveGraphStates();

        if (savingDialog != null)
            savingDialog.dismiss();
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

        Log.i("FA", "Saving instance state");

        outState.putDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA, this.mandelbrotFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA, this.juliaFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS, ((JuliaSeedSettable) this.juliaStrategy).getJuliaSeed());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.i("FA", "Restoring instance state");

        double[] mainGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA);
        double[] juliaGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA);
        double[] juliaParams = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS);

        ((JuliaSeedSettable) this.juliaStrategy).setJuliaSeed(juliaParams[0], juliaParams[1]);
        this.mandelbrotFractalPresenter.setGraphArea(mainGraphArea);
        this.juliaFractalPresenter.setGraphArea(juliaGraphArea);
    }

    // Menu creation and handling

    // Listen for hardware menu presses on older phones, show the menu dialog
    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                this.showMenuDialog();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long timeDiffInMS = (System.nanoTime() - this.sceneStartTime) / 1000000;
        Log.i("FSA", "Time (ms) since start of scene " + timeDiffInMS);

        switch (item.getItemId()) {
            case R.id.showMenu:
                this.showMenuDialog();
                return true;

            case R.id.switchLayout:
                if (timeDiffInMS > 1000)
                    this.toggleLayoutType();
                return true;

            case R.id.switchViews:
                if (timeDiffInMS > 1000)
                    this.onSwapViewsClicked();
                return true;

            default:
                return false;
        }
    }

    public void toggleLayoutType() {
        if (this.layoutType == SceneLayoutEnum.LARGE_SMALL) {
            this.settings.setLayoutType(SceneLayoutEnum.SIDE_BY_SIDE);
        } else {
            this.settings.setLayoutType(SceneLayoutEnum.LARGE_SMALL);
        }
    }

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter, boolean fullRefresh) {
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
        this.firstFractalView.postUIThreadRedraw();
    }

    @Override
    public void onMandelbrotColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender) {
        this.mandelbrotFractalPresenter.fractalStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.mandelbrotFractalPresenter, true);
    }

    @Override
    public void onJuliaColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender) {
        this.juliaFractalPresenter.fractalStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.juliaFractalPresenter, true);
    }

    @Override
    public void onFractalViewReady(IFractalPresenter presenter) {
        Log.i("FA", "Fractal view ready");

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
            Log.i("FA", "Moved default graph area to midpoint of view");
            double[] graphMidPoint = presenter.getGraphPositionFromClickedPosition(view.getWidth() / 2.0f, view.getHeight() / 2.0f);
            double[] originalGraphPoint = presenter.getGraphArea();
            originalGraphPoint[1] -= graphMidPoint[1];
            presenter.setGraphArea(originalGraphPoint);
        }

        this.scheduleRecomputeBasedOnPreferences(presenter, true);
    }

    @Override
    public void onSceneLayoutChanged(SceneLayoutEnum layoutType) {
        this.reloadSelf();
        /*Log.i("FA", "Setting scene layout to " + layoutType.name());
        this.setContentViewFromLayoutType(layoutType);
        this.initialiseToolbar();

        this.mandelbrotFractalPresenter.setView(this.firstFractalView, new Matrix(), this.mandelbrotFractalPresenter);
        this.juliaFractalPresenter.setView(this.secondFractalView, new Matrix(), this.juliaFractalPresenter);

        this.mandelbrotFractalPresenter.onSceneOverlaysChanged(this.sceneOverlays);*/
    }

    public void reloadSelf() {
        this.mandelbrotStrategy.tearDown();
        this.juliaStrategy.tearDown();
        this.firstFractalView.tearDown();
        this.secondFractalView.tearDown();

        this.finish();
        this.startActivity(this.getIntent());
    }
    /*-----------------------------------------------------------------------------------*/
    /*Image saving/sharing*/
    /*-----------------------------------------------------------------------------------*/
   /* TODO: Tidy up this code. Possibly switch to using Handlers and postDelayed.
   */
    //Wait for render to finish, then save the fractal image
    private void saveImage() {
//        cancelledSave = false;
//
//        if (fractalView.isRendering()) {
//            savingDialog = new ProgressDialog(this);
//            savingDialog.setMessage("Waiting for render to finish...");
//            savingDialog.setCancelable(true);
//            savingDialog.setIndeterminate(true);
//            savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                public void onCancel(DialogInterface dialog) {
//                    FractalActivity.this.cancelledSave = true;
//                }
//            });
//            savingDialog.show();
//
//            //Launch a thread to wait for completion
//            new Thread(new Runnable() {
//                public void run() {
//                    if (fractalView.isRendering()) {
//                        while (!cancelledSave && fractalView.isRendering()) {
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                            }
//                        }
//
//                        if (!cancelledSave) {
//                            savingDialog.dismiss();
//                            imagefile = fractalView.saveImage();
//                            String toastText;
//                            if (imagefile == null)
//                                toastText = "Unable to save fractal - filename already in use.";
//                            else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
//                            showToastOnUIThread(toastText, Toast.LENGTH_LONG);
//                        }
//                    }
//                    return;
//                }
//            }).start();
//        } else {
//            imagefile = fractalView.saveImage();
//            String toastText;
//            if (imagefile == null) toastText = "Unable to save fractal - filename already in use.";
//            else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
//            showToastOnUIThread(toastText, Toast.LENGTH_LONG);
//        }
    }

    //Wait for the render to finish, then share the fractal image
    private void shareImage() {
//        cancelledSave = false;
//
//        if (fractalView.isRendering()) {
//            savingDialog = new ProgressDialog(this);
//            savingDialog.setMessage("Waiting for render to finish...");
//            savingDialog.setCancelable(true);
//            savingDialog.setIndeterminate(true);
//            savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                public void onCancel(DialogInterface dialog) {
//                    FractalActivity.this.cancelledSave = true;
//                }
//            });
//            savingDialog.show();
//
//            //Launch a thread to wait for completion
//            new Thread(new Runnable() {
//                public void run() {
//                    if (fractalView.isRendering()) {
//                        while (!cancelledSave && fractalView.isRendering()) {
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                            }
//                        }
//
//                        if (!cancelledSave) {
//                            savingDialog.dismiss();
//                            imagefile = fractalView.saveImage();
//                            if (imagefile != null) {
//                                Intent imageIntent = new Intent(Intent.ACTION_SEND);
//                                imageIntent.setType("image/jpg");
//                                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
//
//                                startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
//                            } else {
//                                showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
//                            }
//                        }
//                    }
//                    return;
//                }
//            }).start();
//        } else {
//            imagefile = fractalView.saveImage();
//
//            if (imagefile != null) {
//                Intent imageIntent = new Intent(Intent.ACTION_SEND);
//                imageIntent.setType("image/png");
//                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));
//
//                startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
//            } else {
//                showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
//            }
//        }
    }

    /*-----------------------------------------------------------------------------------*/
    /*Utilities*/
    /*-----------------------------------------------------------------------------------*/
    /*A single method for running toasts on the UI thread, rather than
       creating new Runnables each time. */
    public void showToastOnUIThread(final String toastText, final int length) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), toastText, length).show();
            }
        });
    }

    /* Show the short tutorial/intro dialog */
    private void showIntro() {
        TextView text = new TextView(this);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setText(Html.fromHtml(getString(R.string.intro_text)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setView(text)
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        ;
        builder.create().show();

        this.settings.setFirstTimeUse(false);
    }

    /* Show the large help dialog */
    private void showHelpDialog() {
        ScrollView scrollView = new ScrollView(this);
        TextView text = new TextView(this);
        text.setText(Html.fromHtml(getString(R.string.help_text)));
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

    private void showMenuDialog() {
        FragmentManager fm = getSupportFragmentManager();
        MenuDialog menuDialog = new MenuDialog();
        menuDialog.show(fm, FRAGMENT_MENU_DIALOG_NAME);
    }

    private void dismissMenuDialog() {
        Fragment dialog = getSupportFragmentManager().findFragmentByTag(FRAGMENT_MENU_DIALOG_NAME);
        if (dialog != null) {
            DialogFragment df = (DialogFragment) dialog;
            df.dismiss();
        }
    }

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

    // Menu Delegate

    @Override
    public void onResetClicked() {
        this.mandelbrotFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultMandelbrotGraphArea);
        this.juliaFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultJuliaGraphArea);

        ((JuliaSeedSettable) this.juliaStrategy).setJuliaSeed(MandelbrotJuliaLocation.defaultJuliaParams[0], MandelbrotJuliaLocation.defaultJuliaParams[1]);
        this.onFractalViewReady(this.mandelbrotFractalPresenter);
        this.onFractalViewReady(this.juliaFractalPresenter);

        this.dismissMenuDialog();
    }

    @Override
    public void onSettingsClicked() {
        this.startActivity(new Intent(this, SettingsActivity.class));
        this.dismissMenuDialog();
    }

    @Override
    public void onDetailClicked() {
        this.dismissMenuDialog();
        this.showDetailDialog();
    }

//    @Override
//    public void onSaveClicked() {
//        this.saveImage();
//        this.dismissMenuDialog();
//    }
//
//    @Override
//    public void onShareClicked() {
//        this.shareImage();
//        this.dismissMenuDialog();
//    }

    @Override
    public void onHelpClicked() {
        this.showHelpDialog();
        this.dismissMenuDialog();
    }

    @Override
    public void onSwitchRendererClicked() {
        double[] juliaParams = ((JuliaSeedSettable) this.juliaStrategy).getJuliaSeed();

        IColourStrategy mandelbrotColourStrategy = this.mandelbrotStrategy.getColourStrategy();
        IColourStrategy juliaColourStrategy = this.juliaStrategy.getColourStrategy();

        this.mandelbrotStrategy.tearDown();
        this.juliaStrategy.tearDown();

        if (this.shouldGPURender) {
            this.mandelbrotStrategy = new MandelbrotCPUFractalComputeStrategy();
            this.juliaStrategy = new JuliaCPUFractalComputeStrategy();
        } else {
            this.mandelbrotStrategy = new MandelbrotGPUFractalComputeStrategy();
            ((GPUFractalComputeStrategy) this.mandelbrotStrategy).setContext(this);

            this.juliaStrategy = new JuliaGPUFractalComputeStrategy();
            ((GPUFractalComputeStrategy) this.juliaStrategy).setContext(this);
        }

        ((JuliaSeedSettable) this.juliaStrategy).setJuliaSeed(juliaParams[0], juliaParams[1]);
        this.mandelbrotStrategy.setColourStrategy(mandelbrotColourStrategy);
        this.juliaStrategy.setColourStrategy(juliaColourStrategy);

        this.mandelbrotFractalPresenter.setComputeStrategy(this.mandelbrotStrategy);
        this.juliaFractalPresenter.setComputeStrategy(this.juliaStrategy);

        this.mandelbrotFractalPresenter.initialiseStrategy();
        this.juliaFractalPresenter.initialiseStrategy();

        this.scheduleRecomputeBasedOnPreferences(this.mandelbrotFractalPresenter, true);
        this.shouldGPURender = !this.shouldGPURender;

        this.dismissMenuDialog();
    }

    @Override
    public void onSwapViewsClicked() {
        this.settings.setViewsSwitched(!this.settings.getViewsSwitched());

        this.reloadSelf();
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

        /*if (atLeastOnePresenterRendering) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbarProgress.setVisibility(View.VISIBLE);
                }
            });
        } else {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toolbarProgress.setVisibility(View.GONE);
                }
            });
        }*/
    }

    @Override
    public void onFractalLongClick(IFractalPresenter presenter, float x, float y) {
        if (presenter != this.mandelbrotFractalPresenter)
            return;

        if (this.showingPinOverlay) {
            this.pinOverlay.setPosition(x, y);

            double[] graphTapPosition = this.mandelbrotFractalPresenter.getGraphPositionFromClickedPosition(x, y);
            this.setJuliaSeedAndRecompute(graphTapPosition, FractalPresenter.DEFAULT_PIXEL_SIZE);
        }
        //Log.i("FA", "First fractal long tap at " + x + " " + y + ", " + graphTapPosition[0] + " " + graphTapPosition[1]);
    }

    private void setJuliaSeedAndRecompute(double[] juliaSeed, int pixelBlockSize) {
        ((JuliaSeedSettable) this.juliaStrategy).setJuliaSeed(juliaSeed[0], juliaSeed[1]);
        this.firstFractalView.postUIThreadRedraw();
        this.juliaFractalPresenter.clearPixelSizes();
        this.juliaFractalPresenter.recomputeGraph(pixelBlockSize);
    }

    @Override
    public void onFractalRecomputeScheduled(IFractalPresenter presenter) {
        if (presenter != this.mandelbrotFractalPresenter)
            return;

        double[] juliaSeed = ((JuliaSeedSettable) this.juliaStrategy).getJuliaSeed();
        double[] newPinPoint = this.mandelbrotFractalPresenter.getPointFromGraphPosition(juliaSeed[0], juliaSeed[1]);

        if (this.showingPinOverlay) {
            this.pinOverlay.setPosition((float) newPinPoint[0], (float) newPinPoint[1]);
        }
    }

    @Override
    public void onFractalRecomputed(IFractalPresenter presenter, double timeTakenInSeconds) {
        Log.i("FP", presenter.getClass().getCanonicalName() + " took " + timeTakenInSeconds + " seconds to finish render");

        String type = "CPU";
        if (this.shouldGPURender)
            type = "GPU";

        String toastText = type + " took " + timeTakenInSeconds + " seconds";
        this.showToastOnUIThread(toastText, toastText.length());
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
        this.juliaFractalPresenter.clearPixelSizes();
        this.juliaFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }
}
