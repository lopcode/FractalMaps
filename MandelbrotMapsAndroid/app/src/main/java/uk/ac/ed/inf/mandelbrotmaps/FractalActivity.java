package uk.ac.ed.inf.mandelbrotmaps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import uk.ac.ed.inf.mandelbrotmaps.AbstractFractalView.FractalViewSize;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDelegate;
import uk.ac.ed.inf.mandelbrotmaps.detail.DetailControlDialog;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuClickDelegate;
import uk.ac.ed.inf.mandelbrotmaps.menu.MenuDialog;

public class FractalActivity extends ActionBarActivity implements OnTouchListener, OnScaleGestureListener,
        OnSharedPreferenceChangeListener, OnLongClickListener, MenuClickDelegate, DetailControlDelegate{

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
    public AbstractFractalView fractalView;
    private AbstractFractalView littleFractalView;
    private View borderView;
    private RelativeLayout relativeLayout;

    // Fractal locations
    private MandelbrotJuliaLocation mjLocation;
    private double[] littleMandelbrotLocation;

    // Dragging/scaling control variables
    private float dragLastX;
    private float dragLastY;
    private int dragID = -1;
    private boolean currentlyDragging = false;

    private ScaleGestureDetector gestureDetector;

    // File saving variables
    private ProgressDialog savingDialog;
    private File imagefile;
    private Boolean cancelledSave = false;

    // Little fractal view tracking
    public boolean showLittleAtStart = true;
    public boolean showingLittle = false;
    private boolean littleFractalSelected = false;

    // Loading spinner (currently all disabled due to slowdown)
    private ProgressBar progressBar;
    private boolean showingSpinner = false;
    private boolean allowSpinner = false;

    public static final String FRAGMENT_MENU_DIALOG_NAME = "menuDialog";
    public static final String FRAGMENT_DETAIL_DIALOG_NAME = "detailControlDialog";

    // Tan Lei variables
    // is the theorem support currently on?
    public boolean tanLeiEnabled = true;
    // should the theorem support be on if it is appropriate?
    public boolean tanLeiToggled = true;
    public boolean TLPointSelected = false;
    public boolean currentlyTLZooming = false;
    public double[][] misPoints = {
            {-2.0, 0.0},
            {0.0, 1.0},
            {-0.10109636384548, -0.95628651080904}};
    public float[][] centerPoints = {
            {-1f, 0f},
            {2f, 0f},
            {-0.30025f, 0.62481f},
            {1.30025f, -0.62481f},
            {-0.32758618f, -0.57776453f},
            {1.32758618f, 0.57776453f}};
    // rotation stored in radians
    public float[][] mandelbrotZoomRotate = {
            // some values multiplied by factor of 2 or 3 to provide noticeable zoom change
            // original values in comments
            {4f, 0f},               // {-2.0, 0.0}
            {2f, 3.14149f},         // {-1f, 0f}
            {4f, 0f},               // {2f, 0f}
            {5.6568f, 0.78540f},    // {0.0, 1.0}
            {1.38642f, 2.0188f},    // {-0.30025f, 0.62481f}
            {2.88516f, 5.83523f},   // {1.30025f, -0.62481f}
            {1.32632f, 2.0854f},    // {-0.10109636384548, -0.95628651080904}
            {1.32834f, 2.0866f},    // {-0.32758618f, -0.57776453f}
            {2.89572f, 0.4105f}     // {1.32758618f, 0.57776453f}
    };

    // Android lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        // If first time launch, show the tutorial/intro
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (prefs.getBoolean(FIRST_TIME_KEY, true)) showIntro();

        Bundle bundle = getIntent().getExtras();

        mjLocation = new MandelbrotJuliaLocation();
        double[] juliaParams = mjLocation.defaultJuliaParams;
        double[] juliaGraphArea = mjLocation.defaultJuliaGraphArea;

        relativeLayout = new RelativeLayout(this);

        //Extract features from bundle, if there is one
        try {
            fractalType = FractalTypeEnum.valueOf(bundle.getString("FractalType"));
            littleMandelbrotLocation = bundle.getDoubleArray("LittleMandelbrotLocation");
            showLittleAtStart = bundle.getBoolean("ShowLittleAtStart");
        } catch (NullPointerException npe) {
        }

        if (fractalType == FractalTypeEnum.MANDELBROT) {
            fractalView = new MandelbrotFractalView(this, FractalViewSize.LARGE);
        } else if (fractalType == FractalTypeEnum.JULIA) {
            fractalView = new JuliaFractalView(this, FractalViewSize.LARGE);
            juliaParams = bundle.getDoubleArray("JuliaParams");
            juliaGraphArea = bundle.getDoubleArray("JuliaGraphArea");
        } else {
            fractalView = new CubicMandelbrotFractalView(this, FractalViewSize.LARGE);
        }

        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        relativeLayout.addView(fractalView, lp);

        Toolbar toolbar = new Toolbar(this);
        relativeLayout.addView(toolbar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        setContentView(relativeLayout);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        mjLocation = new MandelbrotJuliaLocation(juliaGraphArea, juliaParams);
        fractalView.loadLocation(mjLocation);

        gestureDetector = new ScaleGestureDetector(this, this);
    }

    // When destroyed, kill all render threads
    @Override
    protected void onDestroy() {
        super.onDestroy();
        fractalView.stopAllRendering();
        fractalView.interruptThreads();
        if (littleFractalView != null) {
            littleFractalView.stopAllRendering();
            littleFractalView.interruptThreads();
        }
    }

    /* When paused, do the following, dismiss the saving dialog. Might be buggy if mid-save?
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
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

        outState.putDoubleArray(PREVIOUS_MAIN_GRAPH_AREA, fractalView.graphArea);

        if (showingLittle) {
            outState.putDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA, littleFractalView.graphArea);
        }

        if (fractalType == FractalTypeEnum.MANDELBROT) {
            outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((MandelbrotFractalView) fractalView).currentJuliaParams);
        } else {
            outState.putDoubleArray(PREVIOUS_JULIA_PARAMS, ((JuliaFractalView) fractalView).getJuliaParam());
        }

        outState.putBoolean(PREVIOUS_SHOWING_LITTLE, showingLittle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        double[] mainGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_MAIN_GRAPH_AREA);
        double[] littleGraphArea = savedInstanceState.getDoubleArray(PREVIOUS_LITTLE_GRAPH_AREA);
        double[] juliaParams = savedInstanceState.getDoubleArray(PREVIOUS_JULIA_PARAMS);

        MandelbrotJuliaLocation restoredLoc;

        if (fractalType == FractalTypeEnum.MANDELBROT) {
            restoredLoc = new MandelbrotJuliaLocation(mainGraphArea, littleGraphArea, juliaParams);
            ((MandelbrotFractalView) fractalView).currentJuliaParams = juliaParams;
        } else {
            restoredLoc = new MandelbrotJuliaLocation(littleGraphArea, mainGraphArea, juliaParams);
        }

        restoredLoc.setMandelbrotGraphArea(mainGraphArea);
        fractalView.loadLocation(restoredLoc);

        showLittleAtStart = savedInstanceState.getBoolean(PREVIOUS_SHOWING_LITTLE);
    }

    // Set activity result when finishing

    @Override
    public void finish() {
        if (fractalType == FractalTypeEnum.JULIA) {
            double[] juliaParams = ((JuliaFractalView) fractalView).getJuliaParam();
            double[] currentGraphArea = fractalView.graphArea;

            Intent result = new Intent();
            result.putExtra("JuliaParams", juliaParams);
            result.putExtra("JuliaGraphArea", currentGraphArea);

            setResult(Activity.RESULT_OK, result);
        }

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
                if (showingLittle) {
                    double[] juliaGraphArea = data.getDoubleArrayExtra("JuliaGraphArea");
                    double[] juliaParams = data.getDoubleArrayExtra("JuliaParams");
                    littleFractalView.loadLocation(new MandelbrotJuliaLocation(juliaGraphArea, juliaParams));
                }
                break;
        }
    }

    /*-----------------------------------------------------------------------------------*/
    /*Dynamic UI creation*/
    /*-----------------------------------------------------------------------------------*/
   /* Adds the little fractal view and its border, if not showing
    * Also determines its height, width based on large fractal view's size
    */
    public void addLittleView(boolean centre) {
        //Check to see if view has already or should never be included
        if (showingLittle) {
            relativeLayout.bringChildToFront(littleFractalView);
            return;
        }

        //Show a little Julia next to a Mandelbrot and vice versa
        if (fractalType == FractalTypeEnum.MANDELBROT) {
            littleFractalView = new JuliaFractalView(this, FractalViewSize.LITTLE);
        } else {
            littleFractalView = new MandelbrotFractalView(this, FractalViewSize.LITTLE);
        }

        //Set size of border, little view proportional to screen size
        int width = fractalView.getWidth();
        int height = fractalView.getHeight();
        int borderwidth = Math.max(1, (int) (width / 300.0));

        double ratio = (double) width / (double) height;
        width /= 7;
        height = (int) (width / ratio);

        //Add border view (behind little view, slightly larger)
        borderView = new View(this);
        borderView.setBackgroundColor(Color.GRAY);
        LayoutParams borderLayout = new LayoutParams(width + 2 * borderwidth, height + 2 * borderwidth);
        relativeLayout.addView(borderView, borderLayout);

        //Add little fractal view
        LayoutParams lp2 = new LayoutParams(width, height);
        lp2.setMargins(borderwidth, borderwidth, borderwidth, borderwidth);
        relativeLayout.addView(littleFractalView, lp2);

        if (fractalType == FractalTypeEnum.MANDELBROT) {
            littleFractalView.loadLocation(mjLocation);

            double[] jParams;
            if (!centre) {
                jParams = ((MandelbrotFractalView) fractalView).currentJuliaParams;
            } else {
                jParams = ((MandelbrotFractalView) fractalView).getJuliaParams(fractalView.getWidth() / 2, fractalView.getHeight() / 2);
            }

            ((JuliaFractalView) littleFractalView).setJuliaParameter(jParams[0], jParams[1]);
        } else {
            mjLocation.setMandelbrotGraphArea(littleMandelbrotLocation);
            littleFractalView.loadLocation(mjLocation);
        }

        setContentView(relativeLayout);

        showingLittle = true;
    }

    // Hides the little fractal view, if showing
    public void removeLittleView() {
        if (!showingLittle) return;

        relativeLayout.removeView(borderView);
        relativeLayout.removeView(littleFractalView);

        littleFractalView.interruptThreads();

        showingLittle = false;
    }

    /* Shows the progress spinner. Never used because it causes slowdown,
     * leaving it in so I can demonstrate it with benchmarks.
     * Might adapt it to do a progress bar that updates less often.
     */
    public void showProgressSpinner() {
        if (showingSpinner || !allowSpinner) return;

        LayoutParams progressBarParams = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        progressBarParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        progressBar = new ProgressBar(getApplicationContext());
        relativeLayout.addView(progressBar, progressBarParams);
        showingSpinner = true;
    }

    /* As above, except for hiding.
     */
    public void hideProgressSpinner() {
        if (!showingSpinner || !allowSpinner) return;

        runOnUiThread(new Runnable() {

            public void run() {
                relativeLayout.removeView(progressBar);
            }
        });
        showingSpinner = false;
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
        cancelledSave = false;

        if (fractalView.isRendering()) {
            savingDialog = new ProgressDialog(this);
            savingDialog.setMessage("Waiting for render to finish...");
            savingDialog.setCancelable(true);
            savingDialog.setIndeterminate(true);
            savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    FractalActivity.this.cancelledSave = true;
                }
            });
            savingDialog.show();

            //Launch a thread to wait for completion
            new Thread(new Runnable() {
                public void run() {
                    if (fractalView.isRendering()) {
                        while (!cancelledSave && fractalView.isRendering()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }

                        if (!cancelledSave) {
                            savingDialog.dismiss();
                            imagefile = fractalView.saveImage();
                            String toastText;
                            if (imagefile == null)
                                toastText = "Unable to save fractal - filename already in use.";
                            else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
                            showToastOnUIThread(toastText, Toast.LENGTH_LONG);
                        }
                    }
                    return;
                }
            }).start();
        } else {
            imagefile = fractalView.saveImage();
            String toastText;
            if (imagefile == null) toastText = "Unable to save fractal - filename already in use.";
            else toastText = "Saved fractal as " + imagefile.getAbsolutePath();
            showToastOnUIThread(toastText, Toast.LENGTH_LONG);
        }
    }

    //Wait for the render to finish, then share the fractal image
    private void shareImage() {
        cancelledSave = false;

        if (fractalView.isRendering()) {
            savingDialog = new ProgressDialog(this);
            savingDialog.setMessage("Waiting for render to finish...");
            savingDialog.setCancelable(true);
            savingDialog.setIndeterminate(true);
            savingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    FractalActivity.this.cancelledSave = true;
                }
            });
            savingDialog.show();

            //Launch a thread to wait for completion
            new Thread(new Runnable() {
                public void run() {
                    if (fractalView.isRendering()) {
                        while (!cancelledSave && fractalView.isRendering()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }

                        if (!cancelledSave) {
                            savingDialog.dismiss();
                            imagefile = fractalView.saveImage();
                            if (imagefile != null) {
                                Intent imageIntent = new Intent(Intent.ACTION_SEND);
                                imageIntent.setType("image/jpg");
                                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));

                                startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
                            } else {
                                showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
                            }
                        }
                    }
                    return;
                }
            }).start();
        } else {
            imagefile = fractalView.saveImage();

            if (imagefile != null) {
                Intent imageIntent = new Intent(Intent.ACTION_SEND);
                imageIntent.setType("image/png");
                imageIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagefile));

                startActivityForResult(Intent.createChooser(imageIntent, "Share picture using:"), SHARE_IMAGE_REQUEST);
            } else {
                showToastOnUIThread("Unable to share image - couldn't save temporary file", Toast.LENGTH_LONG);
            }
        }
    }

    // Touch controls

    public boolean onTouch(View v, MotionEvent evt) {
        gestureDetector.onTouchEvent(evt);

        switch (evt.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (showingLittle && evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
                    borderView.setBackgroundColor(Color.DKGRAY);
                    littleFractalSelected = true;
                } else if (showingLittle && fractalType == FractalTypeEnum.MANDELBROT && !gestureDetector.isInProgress()
                        && !fractalView.holdingPin && (touchingPin(evt.getX(), evt.getY()))) {
                    // Take hold of the pin, reset the little fractal view.
                    // Prioritize selecting pin over selecting Tan Lei boxes.
                    fractalView.holdingPin = true;
                    updateLittleJulia(evt.getX(), evt.getY());
                } else if (tanLeiEnabled && fractalType == FractalTypeEnum.MANDELBROT) {
                    // user taps "switch" part of box
                    if (touchingInSwitchBox(evt.getX(), evt.getY(), fractalView.getPointOneCoords(),
                            fractalView.switchBoxHeight,fractalView.switchBoxWidth))
                    {
                        // Go to first sample Tan Lei point in Julia set.
                        ((JuliaFractalView) littleFractalView).setJuliaParameter( misPoints[0][0],
                                misPoints[0][1]);
                        littleFractalSelected = true;
                    } else if (touchingInSwitchBox(evt.getX(), evt.getY(), fractalView.getPointTwoCoords(),
                            fractalView.switchBoxHeight,fractalView.switchBoxWidth)) {
                        // Go to second sample Tan Lei point in Julia set.
                        ((JuliaFractalView) littleFractalView).setJuliaParameter( misPoints[1][0],
                             misPoints[1][1]);
                        littleFractalSelected = true;
                    } else if (touchingInSwitchBox(evt.getX(), evt.getY(), fractalView.getPointThreeCoords(),
                            fractalView.switchBoxHeight,fractalView.switchBoxWidth)) {
                        // Go to third sample Tan Lei point in Julia set.
                        ((JuliaFractalView) littleFractalView).setJuliaParameter((double) misPoints[2][0],
                                (double) misPoints[2][1]);
                        littleFractalSelected = true;
                    // user taps "zoom" part of box
                    } else if (touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointOneCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth)
                            || touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointTwoCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth)
                            || touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointThreeCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth)) {
                        currentlyDragging = false;
                        currentlyTLZooming = true;
                        fractalView.stopDragging(true);
                        fractalView.startZooming(evt.getX(), evt.getY());
                        fractalView.zoomImage(evt.getX(), evt.getY(), calculateTLZoom(evt.getX(),evt.getY()));
                    } else {
                        startDragging(evt);
                    }
                } else if (tanLeiEnabled && fractalType == FractalTypeEnum.JULIA &&
                        juliaIsMisPoint()) {
                    // Go back to Mandelbrot
                    if (touchingInJuliaPointBox(evt.getX(), evt.getY())) {
                        // zoom in on point
                        currentlyDragging = false;
                        currentlyTLZooming = true;
                        fractalView.stopDragging(true);
                        fractalView.startZooming(evt.getX(), evt.getY());
                        fractalView.zoomImage(evt.getX(), evt.getY(), calculateTLZoom(evt.getX(),evt.getY()));
                    } else {
                        startDragging(evt);
                    }
                } else {
                    startDragging(evt);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (!gestureDetector.isInProgress()) {
                    if (currentlyDragging) {
                        dragFractal(evt);
                    } else if (showingLittle && !littleFractalSelected && fractalType == FractalTypeEnum.MANDELBROT && fractalView.holdingPin) {
                        updateLittleJulia(evt.getX(), evt.getY());
                    }
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (evt.getPointerCount() == 1)
                    break;
                else {
                    try {
                        chooseNewActivePointer(evt);
                    } catch (IllegalArgumentException iae) {
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                if (currentlyDragging) {
                    stopDragging();
                } else if (littleFractalSelected) {
                    borderView.setBackgroundColor(Color.GRAY);
                    littleFractalSelected = false;
                    if (evt.getX() <= borderView.getWidth() && evt.getY() <= borderView.getHeight()) {
                        if (fractalType == FractalTypeEnum.MANDELBROT) {
                            launchJulia(((JuliaFractalView) littleFractalView).getJuliaParam());
                        } else if (fractalType == FractalTypeEnum.JULIA) {
                            finish();
                        }
                    } else if (tanLeiEnabled && fractalType == FractalTypeEnum.MANDELBROT
                            && (touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointOneCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth))
                            || (touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointTwoCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth))
                            || (touchingInPointBox(evt.getX(), evt.getY(), fractalView.getPointThreeCoords(),
                            fractalView.pointBoxHeight,fractalView.pointBoxWidth))) {
                        if (fractalType == FractalTypeEnum.MANDELBROT) {
                            launchJulia(((JuliaFractalView) littleFractalView).getJuliaParam());
                        } else if (fractalType == FractalTypeEnum.JULIA) {
                            finish();
                        }
                    }
                } else if (currentlyTLZooming) {
                        fractalView.stopZooming();
                        startDragging(evt);
                        currentlyTLZooming = false;
                } else if (fractalView.holdingPin) {
                    // If holding the pin, drop it, update screen (render won't display while dragging, might've finished in background)
                    fractalView.holdingPin = false;
                    updateLittleJulia(evt.getX(), evt.getY());
                }

                fractalView.holdingPin = false;

                break;
        }
        return false;
    }

    /* Find zoom level for Tan Lei points (by finding radius in polar coordinates)
    * Increases zoom by factor of 2 or 3 if too close to 1 to make a difference
    * */
    public float calculateTLZoom(float xPixel, float yPixel) {
        double r = 2.0f;
        if (fractalType == FractalTypeEnum.MANDELBROT) {
            if (touchingInPointBox(xPixel, yPixel, fractalView.getPointOneCoords(),
                    fractalView.pointBoxHeight,fractalView.pointBoxWidth)){
                r = mandelbrotZoomRotate[0][0];
            } else if (touchingInPointBox(xPixel, yPixel, fractalView.getPointTwoCoords(),
                    fractalView.pointBoxHeight,fractalView.pointBoxWidth)){
                r = mandelbrotZoomRotate[3][0];
            } else if (touchingInPointBox(xPixel, yPixel, fractalView.getPointThreeCoords(),
                    fractalView.pointBoxHeight,fractalView.pointBoxWidth)){
                r = mandelbrotZoomRotate[6][0];
            }
        } else if (fractalType == FractalTypeEnum.JULIA) {
            double[] juliaParam = ((JuliaFractalView) fractalView).getJuliaParam();
            for (int offset=0; offset <= 2; offset++) {
                if ((touchingInPointBox(xPixel, yPixel, fractalView.convertCoordsToPixels(centerPoints[offset * 2]),
                        fractalView.pointBoxHeight, fractalView.pointBoxWidth) ) &&
                        juliaParam[0] == misPoints[offset][0] &&
                        juliaParam[1] == misPoints[offset][1]){
                    r = mandelbrotZoomRotate[offset*3+1][0];
                    break;
                } else if (touchingInPointBox(xPixel, yPixel, fractalView.convertCoordsToPixels(centerPoints[(offset * 2) + 1]),
                                fractalView.pointBoxHeight, fractalView.pointBoxWidth) &&
                        juliaParam[0] == misPoints[offset][0] &&
                        juliaParam[1] == misPoints[offset][1]){
                    r = mandelbrotZoomRotate[offset*3+2][0];
                    break;
                } else if (touchingInPointBox(xPixel, yPixel, fractalView.convertDoubleCoordsToPixels(misPoints[offset]),
                                fractalView.pointBoxHeight, fractalView.pointBoxWidth) &&
                        juliaParam[0] == misPoints[offset][0] &&
                        juliaParam[1] == misPoints[offset][1]) {
                    r = mandelbrotZoomRotate[offset*3][0];
                    break;
                }
            }
        }

        return (float) r;
    }

    /* Check to see if input x and y are in one particular point box */
    private boolean touchingInPointBox(float touchX, float touchY, float[] boxCoords, float boxHeight, float boxWidth) {
        return touchX <= boxCoords[0] + boxWidth/2
            && touchX >= boxCoords[0] - boxWidth/2
            && touchY <= boxCoords[1] + boxHeight/2
            && touchY >= boxCoords[1] - boxHeight/2;
    }

    /* Check to see if input x and y are in one particular switch box */
    private boolean touchingInSwitchBox(float touchX, float touchY, float[] boxCoords, float boxHeight, float boxWidth) {
        return touchX <= boxCoords[0] + boxWidth
                && touchX >= boxCoords[0]
                && touchY <= boxCoords[1] + boxHeight
                && touchY >= boxCoords[1];
    }

    /* Check to see if input x and y are in any of the Julia point boxes*/
    private boolean touchingInJuliaPointBox(float touchX, float touchY) {
        double[] juliaParam = ((JuliaFractalView) fractalView).getJuliaParam();
        for (int offset=0; offset <= 2; offset++) {
            if ((touchingInPointBox(touchX, touchY, fractalView.convertCoordsToPixels(centerPoints[offset * 2]),
                    fractalView.pointBoxHeight, fractalView.pointBoxWidth) ||
                    touchingInPointBox(touchX, touchY, fractalView.convertCoordsToPixels(centerPoints[(offset * 2) + 1]),
                            fractalView.pointBoxHeight, fractalView.pointBoxWidth) ||
                    touchingInPointBox(touchX, touchY, fractalView.convertDoubleCoordsToPixels(misPoints[offset]),
                            fractalView.pointBoxHeight, fractalView.pointBoxWidth)) &&
                    juliaParam[0] == misPoints[offset][0] &&
                            juliaParam[1] == misPoints[offset][1]) {
                return true;
            }
        }
        return false;
    }

    /* Check to see if the current Julia parameter is in the list of Misiurewicz points */
    private boolean juliaIsMisPoint() {
        double[] juliaParam = ((JuliaFractalView) fractalView).getJuliaParam();
        return ((juliaParam[0] == misPoints[0][0] && juliaParam[1] == misPoints[0][1]) ||
                (juliaParam[0] == misPoints[1][0] && juliaParam[1] == misPoints[1][1]) ||
                (juliaParam[0] == misPoints[2][0] && juliaParam[1] == misPoints[2][1]));
    }

    private boolean touchingPin(float x, float y) {
        if (fractalType == FractalTypeEnum.JULIA)
            return false;

        boolean touchingPin = false;
        float[] pinCoords = ((MandelbrotFractalView) fractalView).getPinCoords();
        float pinX = pinCoords[0];
        float pinY = pinCoords[1];

        float radius = ((MandelbrotFractalView) fractalView).largePinRadius;

        if (x <= pinX + radius && x >= pinX - radius && y <= pinY + radius && y >= pinY - radius)
            touchingPin = true;

        return touchingPin;
    }


    private void startDragging(MotionEvent evt) {
        dragLastX = (int) evt.getX();
        dragLastY = (int) evt.getY();
        dragID = evt.getPointerId(0);

        fractalView.startDragging();
        currentlyDragging = true;
    }

    private void dragFractal(MotionEvent evt) {
        try {
            int pointerIndex = evt.findPointerIndex(dragID);

            float dragDiffPixelsX = evt.getX(pointerIndex) - dragLastX;
            float dragDiffPixelsY = evt.getY(pointerIndex) - dragLastY;

            // Move the canvas
            if (dragDiffPixelsX != 0.0f && dragDiffPixelsY != 0.0f)
                fractalView.dragFractal(dragDiffPixelsX, dragDiffPixelsY);

            // Update last mouse position
            dragLastX = evt.getX(pointerIndex);
            dragLastY = evt.getY(pointerIndex);
        } catch (Exception iae) {
            // TODO: Investigate why this is in a try-catch block
        }
    }

    private void stopDragging() {
        currentlyDragging = false;
        fractalView.stopDragging(false);
    }

    public boolean onScaleBegin(ScaleGestureDetector detector) {
        fractalView.stopDragging(true);
        fractalView.startZooming(detector.getFocusX(), detector.getFocusY());

        currentlyDragging = false;
        return true;
    }

    public boolean onScale(ScaleGestureDetector detector) {
        fractalView.zoomImage(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
        return true;
    }

    public void onScaleEnd(ScaleGestureDetector detector) {
        fractalView.stopZooming();
        currentlyDragging = true;
        fractalView.startDragging();
    }

    /* Detect a long click, place the Julia pin */
    public boolean onLongClick(View v) {
        // Check that it's not scaling, dragging (check for dragging is a little hacky, but seems to work), or already holding the pin
        if (!gestureDetector.isInProgress() && fractalView.totalDragX < 1 && fractalView.totalDragY < 1 && !fractalView.holdingPin) {
            updateLittleJulia(dragLastX, dragLastY);
            if (currentlyDragging) {
                stopDragging();
            }
            return true;
        }

        return false;
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

    /* Choose a new active pointer from the available ones
     * Used during/at the end of scaling to pick the new dragging pointer*/
    private void chooseNewActivePointer(MotionEvent evt) {
        // Extract the index of the pointer that came up
        final int pointerIndex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = evt.getPointerId(pointerIndex);

        //evt.getX/Y() can apparently throw these exceptions, in some versions of Android (2.2, at least)
        //(https://android-review.googlesource.com/#/c/21318/)
        try {
            dragLastX = (int) evt.getX(dragID);
            dragLastY = (int) evt.getY(dragID);

            if (pointerId == dragID) {
                //Log.d(TAG, "Choosing new active pointer");
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                dragLastX = (int) evt.getX(newPointerIndex);
                dragLastY = (int) evt.getY(newPointerIndex);
                dragID = evt.getPointerId(newPointerIndex);
            }
        } catch (ArrayIndexOutOfBoundsException aie) {
        }
    }


    /* Launches a new Julia fractal activity with the given parameters */
    private void launchJulia(double[] juliaParams) {
        Intent intent = new Intent(this, FractalActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("FractalType", FractalTypeEnum.JULIA.toString());
        bundle.putBoolean("ShowLittleAtStart", true);
        bundle.putDoubleArray("LittleMandelbrotLocation", fractalView.graphArea);

        bundle.putDouble("JULIA_X", juliaParams[0]);
        bundle.putDouble("JULIA_Y", juliaParams[1]);
        bundle.putDoubleArray("JuliaParams", juliaParams);
        bundle.putDoubleArray("JuliaGraphArea", littleFractalView.graphArea);

        intent.putExtras(bundle);
        startActivityForResult(intent, RETURN_FROM_JULIA);
    }

    private void updateLittleJulia(float x, float y) {
        if (fractalType != FractalTypeEnum.MANDELBROT)
            return;

        fractalView.invalidate();

        if (showingLittle) {
            double[] juliaParams = ((MandelbrotFractalView) fractalView).getJuliaParams(x, y);
            ((JuliaFractalView) littleFractalView).setJuliaParameter(juliaParams[0], juliaParams[1]);
        } else {
            ((MandelbrotFractalView) fractalView).getJuliaParams(x, y);
            addLittleView(false);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String changedPref) {
        if (changedPref.equals("MANDELBROT_COLOURS")) {
            String mandelbrotScheme = prefs.getString(changedPref, "MandelbrotDefault");

            if (fractalType == FractalTypeEnum.MANDELBROT) {
                fractalView.setColouringScheme(mandelbrotScheme, true);
            } else if (showingLittle) {
                littleFractalView.setColouringScheme(mandelbrotScheme, true);
            }
        } else if (changedPref.equals("JULIA_COLOURS")) {
            String juliaScheme = prefs.getString(changedPref, "JuliaDefault");

            if (fractalType == FractalTypeEnum.JULIA) {
                fractalView.setColouringScheme(juliaScheme, true);
            } else if (showingLittle) {
                littleFractalView.setColouringScheme(juliaScheme, true);
            }
        } else if (changedPref.equals("PIN_COLOUR")) {
            int newColour = Color.parseColor(prefs.getString(changedPref, "blue"));

            if (fractalType == FractalTypeEnum.MANDELBROT) {
                ((MandelbrotFractalView) fractalView).setPinColour(newColour);
            } else if (showingLittle) {
                ((MandelbrotFractalView) littleFractalView).setPinColour(newColour);
            }
        }
    }

    public double getDetailFromPrefs(FractalViewSize fractalViewSize) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String keyToUse = mandelbrotDetailKey;

        if (fractalType == FractalTypeEnum.MANDELBROT) {
            if (fractalViewSize == FractalViewSize.LARGE)
                keyToUse = mandelbrotDetailKey;
            else
                keyToUse = juliaDetailKey;
        } else {
            if (fractalViewSize == FractalViewSize.LARGE)
                keyToUse = juliaDetailKey;
            else
                keyToUse = mandelbrotDetailKey;
        }

        return (double) prefs.getFloat(keyToUse, (float) AbstractFractalView.DEFAULT_DETAIL_LEVEL);
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
        MandelbrotJuliaLocation bookmark;
        if (fractalType == FractalTypeEnum.MANDELBROT) {
            if (littleFractalView != null) {
                Log.d(TAG, "Showing little...");
                bookmark = new MandelbrotJuliaLocation(fractalView.graphArea, littleFractalView.graphArea,
                        ((MandelbrotFractalView) fractalView).currentJuliaParams);
            } else {
                bookmark = new MandelbrotJuliaLocation(fractalView.graphArea);
            }
        } else {
            bookmark = new MandelbrotJuliaLocation(littleFractalView.graphArea, fractalView.graphArea,
                    ((MandelbrotFractalView) littleFractalView).currentJuliaParams);
        }

        Log.d(TAG, bookmark.toString());

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putString("BOOKMARK", bookmark.toString());
        editor.commit();
    }

    /* Set the current location to the bookmark
     * (Proof-of-concept, currently unused)
     */
    private void loadBookmark() {
        String bookmark = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("BOOKMARK", null);

        if (bookmark != null) {
            Log.d(TAG, "Loaded bookmark " + bookmark);
            MandelbrotJuliaLocation newLocation = new MandelbrotJuliaLocation(bookmark);
            fractalView.loadLocation(newLocation);
        }
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
        this.fractalView.reset();
        this.dismissMenuDialog();
    }

    @Override
    public void onToggleSmallClicked() {
        if (showingLittle) {
            removeLittleView();
            tanLeiEnabled = false;
        } else {
            addLittleView(true);
            if (tanLeiToggled) { tanLeiEnabled = true; }
        }

        this.dismissMenuDialog();
    }

    @Override
    public void onTheoremClicked() {
        if (tanLeiEnabled && tanLeiToggled) { tanLeiEnabled = false; }
        else { tanLeiEnabled = true; }
        tanLeiToggled = !tanLeiToggled;

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

        fractalView.reloadCurrentLocation();
        if (showingLittle)
            littleFractalView.reloadCurrentLocation();
    }

    @Override
    public void onCancelClicked() {
        this.dismissDetailDialog();
    }

}
