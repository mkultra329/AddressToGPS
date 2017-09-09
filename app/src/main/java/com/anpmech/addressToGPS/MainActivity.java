/*
 * The MIT License (MIT)
 * Copyright (c) 2017 Avuton Olrich
 * Copyright (c) 2015 Daniel Barnett
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.anpmech.addressToGPS;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Collection;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    /**
     * This is a XML onClick() callback for the clear button.
     *
     * @param view The clear ImageButton view.
     */
    public void clearButton(final View view) {
        final EditText text = findViewById(R.id.address_form);
        text.getText().clear();
        resetActivity();
    }

    /**
     * This method handles incoming ACTION_VIEW intents.
     *
     * @param intent The Intent containing the link to open.
     */
    private void handleActionView(final Intent intent) {
        String pointOfInterest = intent.getData().getQuery();

        if (pointOfInterest != null) {
            final EditText view = findViewById(R.id.address_form);
            final int equalsIndex = pointOfInterest.indexOf('=');
            pointOfInterest = pointOfInterest.substring(equalsIndex + 1).replace('\n', ',');

            view.setText(pointOfInterest);
        }
    }

    /**
     * This method handles incoming Intents.
     */
    private void handleIntent() {
        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            handleSendText(intent); // Handle text being sent
        } else if (Intent.ACTION_VIEW.equals(action)) {
            handleActionView(intent);
        }
    }

    /**
     * This method handles incoming ACTION_SEND intents.
     *
     * @param intent The intent from another application to be processed.
     */
    private void handleSendText(final Intent intent) {
        final String pointOfInterest = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (pointOfInterest != null) {
            final EditText view = findViewById(R.id.address_form);

            view.setText(pointOfInterest);
        }
    }

    /**
     * This method is called when the navigation button is pressed.
     *
     * @param view The navigation button view.
     */
    public void mapListener(final View view) {
        startActivity((Intent) view.getTag());
    }

    /**
     * This is a XML onClick() callback for the contact ImageButton.
     *
     * @param view The View for the contact ImageButton.
     */
    public void navigateContact(final View view) {
        startActivity((Intent) view.getTag());
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final EditText addressForm = findViewById(R.id.address_form);
        addressForm.setOnEditorActionListener(new AddressFormListener());

        setupWebView();

        handleIntent();
    }

    /**
     * This method handles the logic to reset this Activity according to the API running.
     */
    private void resetActivity() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            final Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
        } else {
            recreate();
        }
    }

    /**
     * This is a XML onClick() callback for the phone ImageButton.
     *
     * @param view The phone ImageButton.
     */
    public void navigatePhone(final View view) {
        startActivity((Intent) view.getTag());
    }

    /**
     * This method sets up the WebView and JavaScript interface.
     */
    private void setupWebView() {
        final WebView webView = findViewById(R.id.webView);
        final JavaScriptInterface jsInterface = new JavaScriptInterface();

        webView.getSettings().setJavaScriptEnabled(true); // enable javascript
        webView.addJavascriptInterface(jsInterface, "JSInterface");
        webView.setWebViewClient(new WebReceivedError());
        webView.loadUrl("file:///android_asset/AddressToGPS/index.html");
        webView.requestFocus();
    }

    /**
     * This is a XML onClick() callback for the share ImageButton.
     *
     * @param view The share ImageButton view.
     */
    public void shareListener(final View view) {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);

        sendIntent.putExtra(Intent.EXTRA_TEXT, (CharSequence) view.getTag());
        sendIntent.setType("text/plain");

        startActivity(Intent.createChooser(sendIntent, "Sharing Coordinates"));
    }

    /**
     * This class watches the user text input and if not empty, enables the lookup button.
     */
    private final class AddressFormListener implements TextView.OnEditorActionListener {

        /**
         * This method handles the input text once the input is ready to be submitted.
         *
         * @param textView The TextView for the address_form.
         * @param i unused.
         * @param keyEvent The KeyEvent which was used to submit the input.
         * @return True if a valid submission method was observed by this callback, false
         * otherwise.
         */
        @Override
        public boolean onEditorAction(final TextView textView, final int i,
                final KeyEvent keyEvent) {
            final boolean actionConsumed;
            final boolean enterPressed = keyEvent != null &&
                    keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                    keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;

            if (enterPressed || i == EditorInfo.IME_ACTION_SEARCH) {
                final WebView webView = findViewById(R.id.webView);

                textView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl("javascript:setFocus(\"" + textView.getText() + "\")");
                webView.requestFocus();
                actionConsumed = true;
            } else {
                actionConsumed = false;
            }

            return actionConsumed;
        }
    }

    /**
     * This class handles runtime errors.
     */
    private final class HandleError implements DialogInterface.OnClickListener, Runnable {

        /**
         * The error message to be shown to the user.
         */
        private final String mErrorMessage;

        /**
         * The default constructor.
         *
         * @param errorMessage The error message to be shown to the user.
         */
        private HandleError(final String errorMessage) {
            mErrorMessage = errorMessage;
            Log.e(TAG, mErrorMessage);
        }

        /**
         * This is a convenience method to use {@code stringRes} as the user error message.
         *
         * @param stringRes The string to resolve to use as the user error message.
         */
        private HandleError(final int stringRes) {
            this(getString(stringRes));
        }

        @Override
        public void onClick(final DialogInterface dialogInterface, final int i) {
            if (i == DialogInterface.BUTTON_POSITIVE) {
                resetActivity();
            } else {
                finish();
            }
        }

        @Override
        public void run() {
            final WebView webView = findViewById(R.id.webView);
            findViewById(R.id.address_form).setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            webView.destroy();

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(mErrorMessage);
            builder.setNegativeButton(R.string.close, this);
            builder.setPositiveButton(R.string.reset, this);
            builder.show();
        }
    }

    /**
     * This class sets up the necessary JavaScript interface to call back to this class upon
     * inquiry.
     */
    private final class JavaScriptInterface {

        @JavascriptInterface
        public void completeReset() {
            runOnUiThread(new HandleError(R.string.load_failed));
        }

        @JavascriptInterface
        public void coordinates(final String name, final String coordinates, final String phone) {
            runOnUiThread(new ParseCoordinates(name, coordinates, phone));
        }
    }

    /**
     * This class parses the results from the Javascript APIs on the UI thread.
     */
    private final class ParseCoordinates implements Runnable {

        // Coordinates in lat,long format.
        private final String mCoordinates;

        private final String mName;

        private final String mPhone;

        private ParseCoordinates(final String name, final String coordinates, final String phone) {
            mCoordinates = coordinates;
            mName = name;
            mPhone = phone;
        }

        private Intent getContactIntent() {
            final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

            if (!"undefined".equals(mPhone)) {
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, mPhone);
            }

            intent.putExtra(ContactsContract.Intents.Insert.POSTAL, mCoordinates);
            intent.putExtra(ContactsContract.Intents.Insert.NAME, mName);

            return intent;
        }

        /**
         * This method returns a ITEF RFC5870 compatible "geo:" Uri
         *
         * @return A standard "geo:" Uri.
         */
        private Uri getMapUri() {
            final String query = mCoordinates + '(' + mName + ')';
            final String encodedQuery = Uri.encode(query);
            return Uri.parse("geo:" + mCoordinates + "?q=" + encodedQuery);
        }

        private void handleClearButton() {
            findViewById(R.id.clear_button).setVisibility(View.VISIBLE);
        }

        private void handleContactButton(final PackageManager manager) {
            final Intent contactIntent = getContactIntent();
            if (contactIntent.resolveActivity(manager) != null) {
                final ImageButton contactButton = findViewById(R.id.navigate_contact);
                contactButton.setVisibility(View.VISIBLE);
                contactButton.setTag(contactIntent);
            }
        }

        private void handleMapButton(final PackageManager manager) {
            final Intent mapIntent = new Intent(Intent.ACTION_VIEW, getMapUri());
            final Collection<ResolveInfo> matchPackages;
            matchPackages = manager.queryIntentActivityOptions(getComponentName(), null, mapIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            // Lacking a map app, don't display a map button.
            if (!matchPackages.isEmpty()) {
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                final ImageButton navigateButton = findViewById(R.id.navigate_map);
                navigateButton.setVisibility(View.VISIBLE);
                navigateButton.setTag(mapIntent);
            }
        }

        private void handleShareButton() {
            final ImageButton shareButton = findViewById(R.id.navigate_share);
            shareButton.setVisibility(View.VISIBLE);
            shareButton.setTag(mCoordinates);
        }

        private void handlePhoneButton(final PackageManager manager) {
            if (!"undefined".equals(mPhone)) {
                final Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + mPhone));

                if (intent.resolveActivity(manager) != null) {
                    final View view = findViewById(R.id.navigate_phone);
                    view.setVisibility(View.VISIBLE);
                    view.setTag(intent);
                }
            }
        }

        @Override
        public void run() {
            final PackageManager manager = getPackageManager();
            final TextView text = findViewById(R.id.gps_coordinates);

            findViewById(R.id.webView).setVisibility(View.GONE);
            text.setText(mName + ": " + '\n' + mCoordinates.replace(",", ", "));
            text.setTag(mCoordinates);
            text.setVisibility(View.VISIBLE);

            handleMapButton(manager);
            handleShareButton();
            handleContactButton(manager);
            handlePhoneButton(manager);
            handleClearButton();
        }
    }

    /**
     * This class is called by the WebView if there is an error.
     */
    private final class WebReceivedError extends WebViewClient {

        @Override
        public void onReceivedError(final WebView view, final int errorCode,
                final String description, final String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                new HandleError(description + ": " + failingUrl).run();
            }
        }

        @Override
        public void onReceivedError(final WebView view, final WebResourceRequest request,
                final WebResourceError error) {
            super.onReceivedError(view, request, error);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                new HandleError(error.getDescription() + ": " + request.getUrl()).run();
            }
        }
    }
}