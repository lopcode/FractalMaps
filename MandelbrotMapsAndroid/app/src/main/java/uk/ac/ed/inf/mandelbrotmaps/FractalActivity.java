package uk.ac.ed.inf.mandelbrotmaps;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
import uk.ac.ed.inf.mandelbrotmaps.colouring.DefaultColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.colouring.JuliaColourStrategy;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDelegate;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDialog;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuClickDelegate;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuDialog;
import uk.ac.ed.inf.mandelbrotmaps.refactor.FractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.refactor.FractalTouchHandler;
import uk.ac.ed.inf.mandelbrotmaps.refactor.FractalView;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalPresenter;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IFractalSceneDelegate;
import uk.ac.ed.inf.mandelbrotmaps.refactor.IPinMovementDelegate;
import uk.ac.ed.inf.mandelbrotmaps.refactor.MandelbrotTouchHandler;
import uk.ac.ed.inf.mandelbrotmaps.refactor.overlay.IFractalOverlay;
import uk.ac.ed.inf.mandelbrotmaps.refactor.overlay.PinOverlay;
import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.JuliaCPUFractalComputeStrategy;
import uk.ac.ed.inf.mandelbrotmaps.refactor.strategies.MandelbrotCPUFractalComputeStrategy;

public class FractalActivity extends ActionBarActivity implements
        OnSharedPreferenceChangeListener, MenuClickDelegate, DetailControlDelegate, IFractalSceneDelegate, IPinMovementDelegate {
    private final String TAG = "MMaps";

    // Constants
    private final int SHARE_IMAGE_REQUEST = 0;
    private final int RETURN_FROM_JULIA = 1;

    // Shared pref keys
    public static final String mandelbrotDetailKey = "MANDELBROT_DETAIL";
    public static final String juliaDetailKey = "JULIA_DETAIL";
    public static final String DETAIL_CHANGED_KEY = "DETAIL_CHANGED";
    private final String PREVIOUS_MAIN_GRAPH_AREA = "prevMainGraphArea";
    private final String PREVIOUS_LITTLE_GRAPH_AREA = "prevLittleGraphArea";
    private final String PREVIOUS_JULIA_PARAMS = "prevJuliaParams";
    private final String PREVIOUS_SHOWING_LITTLE = "prevShowingLittle";
    private final String FIRST_TIME_KEY = "FirstTime";

    public FractalTypeEnum fractalType = FractalTypeEnum.MANDELBROT;

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

    JuliaCPUFractalComputeStrategy juliaStrategy;

    // Fractal locations
    private MandelbrotJuliaLocation mjLocation;
    private double[] littleMandelbrotLocation;

    // File saving variables
    private ProgressDialog savingDialog;
    private File imagefile;
    private Boolean cancelledSave = false;

    public static final String FRAGMENT_MENU_DIALOG_NAME = "menuDialog";
    public static final String FRAGMENT_DETAIL_DIALOG_NAME = "detailControlDialog";

    private HashMap<IFractalPresenter, Boolean> UIRenderStates = new HashMap<IFractalPresenter, Boolean>();

    // Overlays
    private List<IFractalOverlay> sceneOverlays;
    private PinOverlay pinOverlay;

    // Android lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.fractals_side_by_side);
        ButterKnife.inject(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        // If first time launch, show the tutorial/intro
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (prefs.getBoolean(FIRST_TIME_KEY, true)) showIntro();

        Bundle bundle = getIntent().getExtras();

        mjLocation = new MandelbrotJuliaLocation();
        double[] juliaParams = mjLocation.defaultJuliaParams;
        double[] juliaGraphArea = mjLocation.defaultJuliaGraphArea;

        //Extract features from bundle, if there is one
        try {
            fractalType = FractalTypeEnum.valueOf(bundle.getString("FractalType"));
            littleMandelbrotLocation = bundle.getDoubleArray("LittleMandelbrotLocation");
//            showLittleAtStart = bundle.getBoolean("ShowLittleAtStart");
        } catch (NullPointerException npe) {
        }


        firstFractalView.initialise();
        secondFractalView.initialise();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        this.firstFractalPresenter = new FractalPresenter(this, this, new MandelbrotCPUFractalComputeStrategy());
        MandelbrotTouchHandler mandelbrotTouchHandler = new MandelbrotTouchHandler(this, this.firstFractalPresenter);
        mandelbrotTouchHandler.setPinMovementDelegate(this);
        this.firstFractalPresenter.setTouchHandler(mandelbrotTouchHandler);
        this.firstFractalPresenter.fractalStrategy.setColourStrategy(new DefaultColourStrategy());
        this.firstFractalPresenter.setFractalDetail(this.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.firstFractalView.setResizeListener(this.firstFractalPresenter);

        this.firstFractalPresenter.setView(this.firstFractalView, new Matrix(), this.firstFractalPresenter);

        juliaStrategy = new JuliaCPUFractalComputeStrategy();
        juliaStrategy.setJuliaSeed(juliaParams[0], juliaParams[1]);
        this.secondFractalPresenter = new FractalPresenter(this, this, juliaStrategy);
        this.secondFractalPresenter.setTouchHandler(new FractalTouchHandler(this, this.secondFractalPresenter));
        this.secondFractalPresenter.fractalStrategy.setColourStrategy(new JuliaColourStrategy());
        this.secondFractalPresenter.setFractalDetail(this.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.secondFractalView.setResizeListener(this.secondFractalPresenter);

        this.secondFractalPresenter.setView(this.secondFractalView, new Matrix(), this.secondFractalPresenter);

        this.initialiseOverlays();

        mjLocation = new MandelbrotJuliaLocation(juliaGraphArea, juliaParams);
        this.firstFractalPresenter.setGraphArea(mjLocation.defaultMandelbrotGraphArea);
        this.secondFractalPresenter.setGraphArea(mjLocation.defaultJuliaGraphArea);

        this.UIRenderStates.put(this.firstFractalPresenter, false);
        this.UIRenderStates.put(this.secondFractalPresenter, false);
    }

    public void initialiseOverlays() {
        this.sceneOverlays = new ArrayList<IFractalOverlay>();
        this.pinOverlay = new PinOverlay(this, R.color.dark_blue, R.color.black, 42.0f, 100f, 100f);
        this.sceneOverlays.add(this.pinOverlay);
        this.firstFractalPresenter.onSceneOverlaysChanged(this.sceneOverlays);
    }

    // When destroyed, kill all render threads
    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.firstFractalPresenter.fractalStrategy.tearDown();
        this.secondFractalPresenter.fractalStrategy.tearDown();
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
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

//        outState.putDoubleArray(PREVIOUS_MAIN_GRAPH_AREA, fractalView.graphArea);
//
//        if (showingLittle) {
//            outState.putDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA, littleFractalView.graphArea);
//        }
//
//        if (fractalType == FractalTypeEnum.MANDELBROT) {
//            outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((MandelbrotFractalView) fractalView).currentJuliaParams);
//        } else {
//            //outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((JuliaFractalView) fractalView).getJuliaParam());
//        }
//
//        outState.putBoolean(PREVIOUS_SHOWING_LITTLE, showingLittle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        double[] mainGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_MAIN_GRAPH_AREA);
        double[] littleGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA);
        double[] juliaParams = savedInstanceState.getDoubleArray(PREVIOUS_JULIA_PARAMS);

//        MandelbrotJuliaLocation restoredLoc;
//
//        if (fractalType == FractalTypeEnum.MANDELBROT) {
//            restoredLoc = new MandelbrotJuliaLocation(mainGraphArea, littleGraphArea, juliaParams);
//            ((MandelbrotFractalView) fractalView).currentJuliaParams = juliaParams;
//        } else {
//            restoredLoc = new MandelbrotJuliaLocation(littleGraphArea, mainGraphArea, juliaParams);
//        }
//
//        restoredLoc.setMandelbrotGraphArea(mainGraphArea);
//        fractalView.loadLocation(restoredLoc);
//
//        showLittleAtStart = savedInstanceState.getBoolean(PREVIOUS_SHOWING_LITTLE);
    }

    // Set activity result when finishing

    @Override
    public void finish() {
//        if (fractalType == FractalTypeEnum.JULIA) {
//            //double[] juliaParams = ((JuliaFractalView) fractalView).getJuliaParam();
//            double[] currentGraphArea = fractalView.graphArea;
//
//            Intent result = new Intent();
//            //result.putExtra("JuliaParams", juliaParams);
//            result.putExtra("JuliaGraphArea", currentGraphArea);
//
//            setResult(Activity.RESULT_OK, result);
//        }

        super.finish();
    }

    //Get result of launched activity (only time used is after sharing, so delete temp. image)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case SHARE_IMAGE_REQUEST:
                // Delete the temporary image
                imagefile.delete();
                break;

            case RETURN_FROM_JULIA:
//                if (showingLittle) {
//                    double[] juliaGraphArea = data.getDoubleArrayExtra("JuliaGraphArea");
//                    double[] juliaParams = data.getDoubleArrayExtra("JuliaParams");
//                    littleFractalView.loadLocation(new MandelbrotJuliaLocation(juliaGraphArea, juliaParams));
//                }
                break;
        }
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

            default:
                return false;
        }
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


    /* Launches a new Julia fractal activity with the given parameters */
//    private void launchJulia(double[] juliaParams) {
//        Intent intent = new Intent(this, FractalActivity.class);
//        Bundle bundle = new Bundle();
//        bundle.putString("FractalType", FractalTypeEnum.JULIA.toString());
//        bundle.putBoolean("ShowLittleAtStart", true);
//        bundle.putDoubleArray("LittleMandelbrotLocation", fractalView.graphArea);
//
//        bundle.putDouble("JULIA_X", juliaParams[0]);
//        bundle.putDouble("JULIA_Y", juliaParams[1]);
//        bundle.putDoubleArray("JuliaParams", juliaParams);
//        bundle.putDoubleArray("JuliaGraphArea", littleFractalView.graphArea);
//
//        intent.putExtras(bundle);
//        startActivityForResult(intent, RETURN_FROM_JULIA);
//    }

    private void updateLittleJulia(float x, float y) {
        if (fractalType != FractalTypeEnum.MANDELBROT)
            return;

//        fractalView.invalidate();
//
//        littleFractalView.strategy.clearPixelSizes();
//        double[] juliaParams = ((MandelbrotFractalView) fractalView).getJuliaParams(x, y);
//        ((JuliaFractalView) littleFractalView).setJuliaParameter(juliaParams[0], juliaParams[1]);


//        Log.i("FA", "Setting julia params to " + juliaParams[0] + " " + juliaParams[1]);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String changedPref) {
//        if (changedPref.equals("MANDELBROT_COLOURS")) {
//            String mandelbrotScheme = prefs.getString(changedPref, "MandelbrotDefault");
//
//            if (fractalType == FractalTypeEnum.MANDELBROT) {
//                this.firstFractalPresenter.setColouringScheme(mandelbrotScheme, true);
//            } else if (showingLittle) {
//                littleFractalView.setColouringScheme(mandelbrotScheme, true);
//            }
//        } else if (changedPref.equals("JULIA_COLOURS")) {
//            String juliaScheme = prefs.getString(changedPref, "JuliaDefault");
//
//            if (fractalType == FractalTypeEnum.JULIA) {
//                fractalView.setColouringScheme(juliaScheme, true);
//            } else if (showingLittle) {
//                littleFractalView.setColouringScheme(juliaScheme, true);
//            }
//        } else if (changedPref.equals("PIN_COLOUR")) {
//            int newColour = Color.parseColor(prefs.getString(changedPref, "blue"));
//
//            if (fractalType == FractalTypeEnum.MANDELBROT) {
//                ((MandelbrotFractalView) fractalView).setPinColour(newColour);
//            } else if (showingLittle) {
//                //((MandelbrotFractalView) littleFractalView).setPinColour(newColour);
//            }
//        }
    }

    public double getDetailFromPrefs(FractalTypeEnum fractalTypeEnum) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyToUse = mandelbrotDetailKey;

        if (fractalTypeEnum == FractalTypeEnum.MANDELBROT) {

            keyToUse = mandelbrotDetailKey;

        } else {

            keyToUse = juliaDetailKey;

        }

        return (double) prefs.getFloat(keyToUse, (float) FractalPresenter.DEFAULT_DETAIL_LEVEL);
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

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putBoolean(FIRST_TIME_KEY, false);
        editor.commit();
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

    /* Set the bookmark location in Prefs to the current location
     * (Proof-of-concept, currently unused)
     */
    private void setBookmark() {
//        MandelbrotJuliaLocation bookmark;
//        if (fractalType == FractalTypeEnum.MANDELBROT) {
//            if (littleFractalView != null) {
//                Log.d(TAG, "Showing little...");
//                bookmark = new MandelbrotJuliaLocation(fractalView.graphArea, littleFractalView.graphArea,
//                        ((MandelbrotFractalView) fractalView).currentJuliaParams);
//            } else {
//                bookmark = new MandelbrotJuliaLocation(fractalView.graphArea);
//            }
//        } else {
//            //bookmark = new MandelbrotJuliaLocation(littleFractalView.graphArea, fractalView.graphArea,
//            //        ((MandelbrotFractalView) littleFractalView).currentJuliaParams);
//        }
//
//        //Log.d(TAG, bookmark.toString());
//
//        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
//        //editor.putString("BOOKMARK", bookmark.toString());
//        editor.commit();
    }

    /* Set the current location to the bookmark
     * (Proof-of-concept, currently unused)
     */
    private void loadBookmark() {
//        String bookmark = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("BOOKMARK", null);
//
//        if (bookmark != null) {
//            Log.d(TAG, "Loaded bookmark " + bookmark);
//            MandelbrotJuliaLocation newLocation = new MandelbrotJuliaLocation(bookmark);
//            fractalView.loadLocation(newLocation);
//        }
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
        this.firstFractalPresenter.setGraphArea(new MandelbrotJuliaLocation().defaultMandelbrotGraphArea);
        this.firstFractalPresenter.clearPixelSizes();
        this.firstFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);

        this.secondFractalPresenter.setGraphArea(new MandelbrotJuliaLocation().defaultJuliaGraphArea);
        this.secondFractalPresenter.clearPixelSizes();
        this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);

        this.dismissMenuDialog();
    }

    @Override
    public void onToggleSmallClicked() {
        Log.e("FA", "No-op on toggle small clicked");

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

    @Override
    public void onSaveClicked() {
        this.saveImage();
        this.dismissMenuDialog();
    }

    @Override
    public void onShareClicked() {
        this.shareImage();
        this.dismissMenuDialog();
    }

    @Override
    public void onHelpClicked() {
        this.showHelpDialog();
        this.dismissMenuDialog();
    }

    // Detail control delegate

    @Override
    public void onApplyChangesClicked() {
        this.dismissDetailDialog();

        this.firstFractalPresenter.setFractalDetail(this.getDetailFromPrefs(FractalTypeEnum.MANDELBROT));
        this.secondFractalPresenter.setFractalDetail(this.getDetailFromPrefs(FractalTypeEnum.JULIA));
        this.firstFractalPresenter.clearPixelSizes();
        this.secondFractalPresenter.clearPixelSizes();
        this.firstFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
        this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
//        fractalView.reloadCurrentLocation();
//        if (showingLittle)
//            littleFractalView.reloadCurrentLocation();
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
        this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }

    @Override
    public void onFractalRecomputed(IFractalPresenter presenter) {
        if (presenter != this.firstFractalPresenter)
            return;

        double[] juliaSeed = this.juliaStrategy.getJuliaSeed();
        double[] newPinPoint = this.firstFractalPresenter.getPointFromGraphPosition(juliaSeed[0], juliaSeed[1]);

        this.pinOverlay.setPosition((float) newPinPoint[0], (float) newPinPoint[1]);
    }

    // IPinMovementDelegate

    @Override
    public void pinDragged(float x, float y) {
        this.pinOverlay.setPosition(x, y);
        double[] graphTapPosition = this.firstFractalPresenter.getGraphPositionFromClickedPosition(x, y);
        this.setJuliaSeedAndRecompute(graphTapPosition, FractalPresenter.CRUDE_PIXEL_BLOCK);
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
    public void stoppedDraggingPin() {
        this.pinOverlay.setHilighted(false);
        this.secondFractalPresenter.clearPixelSizes();
        this.secondFractalPresenter.recomputeGraph(FractalPresenter.DEFAULT_PIXEL_SIZE);
    }
}
