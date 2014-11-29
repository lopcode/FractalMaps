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
import android.widget.ProgressBar;
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
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.cpu.JuliaCPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.cpu.MandelbrotCPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.compute.strategies.gpu.GPUFractalComputeStrategy;
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

public class FractalActivity extends ActionBarActivity implements IFractalSceneDelegate, IPinMovementDelegate, MenuClickDelegate, DetailControlDelegate {
    // Layout variables
    @InjectView(R.id.toolbar)
    Toolbar toolbar;

    @InjectView(R.id.toolbarProgress)
    ProgressBar toolbarProgress;

    @InjectView(R.id.firstFractalView)
    FractalView firstFractalView;

    @InjectView(R.id.secondFractalView)
    FractalView secondFractalView;

    FractalPresenter firstFractalPresenter;
    FractalPresenter secondFractalPresenter;

    IFractalComputeStrategy mandelbrotStrategy;
    JuliaCPUFractalComputeStrategy juliaStrategy;

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

    // Android lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        Log.i("FA", "OnCreate");

        this.settings = new SettingsManager(this);
        this.settings.setFractalSceneDelegate(this);

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(this.settings);

        this.layoutType = this.settings.getLayoutType();
        this.setContentViewFromLayoutType(this.layoutType);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        if (this.settings.isFirstTimeUse()) {
            showIntro();
        }

        this.firstFractalView.initialise();
        this.secondFractalView.initialise();

        this.initialiseToolbar();

        this.initialiseMandelbrotPresenter();
        this.initialiseJuliaPresenter();

        this.initialiseOverlays();
        this.settings.refreshColourSettings();

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

        this.initialiseFractalParameters(mainGraphArea, juliaGraphArea, juliaParams);
        this.initialiseRenderStates();
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
        this.mandelbrotStrategy = new GPUFractalComputeStrategy();
        ((GPUFractalComputeStrategy) this.mandelbrotStrategy).setContext(this);
//        this.mandelbrotStrategy = new MandelbrotCPUFractalComputeStrategy();
        this.firstFractalPresenter = new FractalPresenter(this, this, mandelbrotStrategy);
        MandelbrotTouchHandler mandelbrotTouchHandler = new MandelbrotTouchHandler(this, this.firstFractalPresenter);
        mandelbrotTouchHandler.setPinMovementDelegate(this);
        this.firstFractalPresenter.setTouchHandler(mandelbrotTouchHandler);
//        this.firstFractalPresenter.fractalStrategy.setColourStrategy(new DefaultColourStrategy());
        this.firstFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.firstFractalView.setResizeListener(this.firstFractalPresenter);

        this.firstFractalPresenter.setView(this.firstFractalView, new Matrix(), this.firstFractalPresenter);
    }

    public void initialiseJuliaPresenter() {
        juliaStrategy = new JuliaCPUFractalComputeStrategy();
        this.secondFractalPresenter = new FractalPresenter(this, this, juliaStrategy);
        this.secondFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.secondFractalPresenter));
//        this.secondFractalPresenter.fractalStrategy.setColourStrategy(new JuliaColourStrategy());
        this.secondFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.secondFractalView.setResizeListener(this.secondFractalPresenter);

        this.secondFractalPresenter.setView(this.secondFractalView, new Matrix(), this.secondFractalPresenter);
    }

    public void initialiseFractalParameters(double[] mandelbrotGraphArea, double[] juliaGraphArea, double[] juliaSeed) {
        this.firstFractalPresenter.setGraphArea(mandelbrotGraphArea);
        this.secondFractalPresenter.setGraphArea(juliaGraphArea);
        this.juliaStrategy.setJuliaSeed(juliaSeed[0], juliaSeed[1]);
    }

    public void initialiseRenderStates() {
        this.UIRenderStates.put(this.firstFractalPresenter, false);
        this.UIRenderStates.put(this.secondFractalPresenter, false);
    }

    public void initialiseOverlays() {
        this.sceneOverlays = new ArrayList<IFractalOverlay>();
        this.pinOverlay = new PinOverlay(this, 42.0f, 100f, 100f);
        this.settings.refreshPinSettings();
        this.sceneOverlays.add(this.pinOverlay);
        this.firstFractalPresenter.onSceneOverlaysChanged(this.sceneOverlays);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        double[] mandelbrotGraphArea = this.firstFractalPresenter.getGraphArea();
        double[] juliaGraphArea = this.secondFractalPresenter.getGraphArea();
        double[] juliaParams = this.juliaStrategy.getJuliaSeed();

        this.settings.savePreviousMandelbrotGraph(new SavedGraphArea(mandelbrotGraphArea[0], mandelbrotGraphArea[1], mandelbrotGraphArea[2]));
        this.settings.savePreviousJuliaGraph(new SavedJuliaGraph(juliaGraphArea[0], juliaGraphArea[1], juliaGraphArea[2], juliaParams[0], juliaParams[1]));

        this.firstFractalPresenter.fractalStrategy.tearDown();
        this.secondFractalPresenter.fractalStrategy.tearDown();

        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this.settings);
    }

    @Override
    protected void onPause() {
        super.onPause();

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

        outState.putDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA, this.firstFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA, this.secondFractalPresenter.getGraphArea());
        outState.putDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS, this.juliaStrategy.getJuliaSeed());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.i("FA", "Restoring instance state");

        double[] mainGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_MAIN_GRAPH_AREA);
        double[] juliaGraphArea = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_LITTLE_GRAPH_AREA);
        double[] juliaParams = savedInstanceState.getDoubleArray(SettingsManager.PREVIOUS_JULIA_PARAMS);

        this.juliaStrategy.setJuliaSeed(juliaParams[0], juliaParams[1]);
        this.firstFractalPresenter.setGraphArea(mainGraphArea);
        this.secondFractalPresenter.setGraphArea(juliaGraphArea);
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
        switch (item.getItemId()) {
            case R.id.showMenu:
                this.showMenuDialog();
                return true;

            case R.id.switchLayout:
                this.onSwitchLayoutClicked();
                return true;

            default:
                return false;
        }
    }

    public void scheduleRecomputeBasedOnPreferences(IFractalPresenter presenter) {
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
        this.firstFractalPresenter.fractalStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.firstFractalPresenter);
    }

    @Override
    public void onJuliaColourSchemeChanged(IColourStrategy colourStrategy, boolean reRender) {
        this.secondFractalPresenter.fractalStrategy.setColourStrategy(colourStrategy);
        if (reRender)
            this.scheduleRecomputeBasedOnPreferences(this.secondFractalPresenter);
    }

    @Override
    public void onFractalViewReady(IFractalPresenter presenter) {
        Log.i("FA", "Fractal view ready");

        // Move the fractal down a to the mid point of the view
        //  Only if the graph area is the default, otherwise it got set manually
        View view = null;
        boolean isDefaultGraphArea = false;
        if (presenter == this.firstFractalPresenter) {
            view = this.firstFractalView;
            isDefaultGraphArea = this.firstFractalPresenter.getGraphArea() == MandelbrotJuliaLocation.defaultMandelbrotGraphArea;
        } else if (presenter == this.secondFractalPresenter) {
            view = this.secondFractalView;
            isDefaultGraphArea = this.secondFractalPresenter.getGraphArea() == MandelbrotJuliaLocation.defaultJuliaGraphArea;
        }

        if (isDefaultGraphArea) {
            Log.i("FA", "Moved default graph area to midpoint of view");
            double[] graphMidPoint = presenter.getGraphPositionFromClickedPosition(view.getWidth() / 2.0f, view.getHeight() / 2.0f);
            double[] originalGraphPoint = presenter.getGraphArea();
            originalGraphPoint[1] -= graphMidPoint[1];
            presenter.setGraphArea(originalGraphPoint);
        }

        this.scheduleRecomputeBasedOnPreferences(presenter);
    }

    @Override
    public void onSceneLayoutChanged(SceneLayoutEnum layoutType) {
        Log.i("FA", "Setting scene layout to " + layoutType.name());
        this.setContentViewFromLayoutType(layoutType);
        this.initialiseToolbar();

        this.firstFractalPresenter.setView(this.firstFractalView, new Matrix(), this.firstFractalPresenter);
        this.secondFractalPresenter.setView(this.secondFractalView, new Matrix(), this.secondFractalPresenter);

        this.firstFractalPresenter.onSceneOverlaysChanged(this.sceneOverlays);
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
        this.firstFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultMandelbrotGraphArea);
        this.secondFractalPresenter.setGraphArea(MandelbrotJuliaLocation.defaultJuliaGraphArea);

        this.juliaStrategy.setJuliaSeed(MandelbrotJuliaLocation.defaultJuliaParams[0], MandelbrotJuliaLocation.defaultJuliaParams[1]);
        this.onFractalViewReady(this.firstFractalPresenter);
        this.onFractalViewReady(this.secondFractalPresenter);

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
    public void onSwitchLayoutClicked() {
        if (this.layoutType == SceneLayoutEnum.LARGE_SMALL) {
            this.settings.setLayoutType(SceneLayoutEnum.SIDE_BY_SIDE);
        } else {
            this.settings.setLayoutType(SceneLayoutEnum.LARGE_SMALL);
        }

        this.dismissMenuDialog();
    }

    // Detail control delegate

    @Override
    public void onApplyChangesClicked() {
        this.dismissDetailDialog();

        this.firstFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.secondFractalPresenter.setFractalDetail(this.settings.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.scheduleRecomputeBasedOnPreferences(this.firstFractalPresenter);
        this.scheduleRecomputeBasedOnPreferences(this.secondFractalPresenter);
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
        }
    }

    @Override
    public void onFractalLongClick(IFractalPresenter presenter, float x, float y) {
        if (presenter != this.firstFractalPresenter)
            return;

        this.pinOverlay.setPosition(x, y);
        double[] graphTapPosition = this.firstFractalPresenter.getGraphPositionFromClickedPosition(x, y);
        this.setJuliaSeedAndRecompute(graphTapPosition, FractalPresenter.DEFAULT_PIXEL_SIZE);
        //Log.i("FA", "First fractal long tap at " + x + " " + y + ", " + graphTapPosition[0] + " " + graphTapPosition[1]);

    }

    private void setJuliaSeedAndRecompute(double[] juliaSeed, int pixelBlockSize) {
        this.juliaStrategy.setJuliaSeed(juliaSeed[0], juliaSeed[1]);
        this.firstFractalView.postUIThreadRedraw();
        this.secondFractalPresenter.clearPixelSizes();
        this.secondFractalPresenter.recomputeGraph(pixelBlockSize);
    }

    @Override
    public void onFractalRecomputeScheduled(IFractalPresenter presenter) {
        if (presenter != this.firstFractalPresenter)
            return;

        double[] juliaSeed = this.juliaStrategy.getJuliaSeed();
        double[] newPinPoint = this.firstFractalPresenter.getPointFromGraphPosition(juliaSeed[0], juliaSeed[1]);

        this.pinOverlay.setPosition((float) newPinPoint[0], (float) newPinPoint[1]);
    }

    @Override
    public void onFractalRecomputed(IFractalPresenter presenter, double timeInSeconds) {
        Log.i("FP", presenter.getClass().getCanonicalName() + " took " + timeInSeconds + " seconds to finish render");

        String toastText = "Recompute took " + timeInSeconds + " seconds";
        this.showToastOnUIThread(toastText, toastText.length());
    }

    // IPinMovementDelegate

    @Override
    public void pinDragged(float x, float y, boolean forceUpdate) {
        this.pinOverlay.setPosition(x, y);
        this.firstFractalView.postUIThreadRedraw();

        double dragDistance = Math.sqrt(Math.pow(this.previousPinDragX - x, 2) + Math.pow(this.previousPinDragY - y, 2));
        if (dragDistance > 1 || forceUpdate) {
            double[] graphTapPosition = this.firstFractalPresenter.getGraphPositionFromClickedPosition(x, y);
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
        this.secondFractalPresenter.clearPixelSizes();
        this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }
}
