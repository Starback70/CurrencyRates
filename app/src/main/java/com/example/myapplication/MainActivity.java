package com.example.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity {

    private Map<String, Currency> currency = new TreeMap<>();
    private List<String> currencyCharCode = new ArrayList<>(Arrays.asList("Выберите валюту"));
    private TextView currencyCode;
    private TextView currencyName;
    private TextView currencyValue;
    private TextView currencyList;
    private TextView convertResult;
    private Button updateButton;
    private Button convertButton;
    private EditText convertValue;
    private double result;
    private int currencyChoice1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        updateCurrencyRates();
        currencyList = findViewById(R.id.tv_currency_list);
//        currencyCode = findViewById(R.id.tv_currency_code);
//        currencyName = findViewById(R.id.tv_currency_name);
//        currencyValue = findViewById(R.id.tv_currency_value);
        updateButton = findViewById(R.id.updateCurrency);
        View.OnClickListener onClickListenerUpdate = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCurrencyRates();
                Toast.makeText(getBaseContext(), "Курсы валют обновлены", Toast.LENGTH_SHORT).show();
            }
        };
        updateButton.setOnClickListener(onClickListenerUpdate);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyCharCode);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = findViewById(R.id.s_currency);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currencyChoice1 = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        convertButton = findViewById(R.id.bt_convert);
        convertResult = findViewById(R.id.tv_result);
        View.OnClickListener onClickListenerConvert = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convertValue = findViewById(R.id.et_value);
                if (currencyChoice1 > 0) {
                    if (convertValue.getText().toString().length() == 0) {
                        convertValue.setError("Введи сумму в рублях");
                    } else {
                        double value = Double.parseDouble(convertValue.getText().toString());
                        double rate = Objects.requireNonNull(currency
                                .get(currencyCharCode.get(currencyChoice1))).getValue();
                        System.out.println("value: " + value);
                        System.out.println("rate: " + rate);
                        result = value / rate;
                        convertResult.setText(String.valueOf(new DecimalFormat("#.##").format(result)));
                    }
                } else {
                    Toast.makeText(getBaseContext(), "Выберите валюту для конвертации", Toast.LENGTH_SHORT).show();
                }
            }
        };
        convertButton.setOnClickListener(onClickListenerConvert);

    }

    public static String getResponseFromURL(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");
            if (scanner.hasNext()) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    class CBTask extends AsyncTask<URL, Void, String> {
        @Override
        protected String doInBackground(URL... urls) {
            String response = null;
            try {
                response = getResponseFromURL(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        protected void onPostExecute(String response) {
            try {
                JSONObject jsonResponse = new JSONObject(response);
                JSONObject jsonValute = jsonResponse.getJSONObject("Valute");
                System.out.println("jsonValute.length(): " + jsonValute.length());
                Iterator<String> iterator = jsonValute.keys();
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    currency.put(next, new Currency(Objects.requireNonNull(jsonValute.optJSONObject(next))));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            currencyList.setText(getListCurrency());
        }
    }

    public String getListCurrency() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Currency> entry : currency.entrySet()) {
            String charCode = entry.getValue().getCharCode();
            String name = entry.getValue().getName();
            currencyCharCode.add(charCode);
            sb
                    .append(charCode)
                    .append(" ")
                    .append(name)
                    .append(" ")
                    .append(entry.getValue().getValue())
                    .append("\n");
        }
        return sb.toString();
    }

    private void updateCurrencyRates() {
        try {
            new CBTask().execute(new URL("https://www.cbr-xml-daily.ru/daily_json.js"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
