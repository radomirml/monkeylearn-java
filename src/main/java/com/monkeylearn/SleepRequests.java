package com.monkeylearn;

import org.apache.http.Header;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SleepRequests {

    private String token;

    public SleepRequests(String token) {
        this.token = token;
    }

    public Tuple<JSONObject, Header[]> makeRequest(String url, String method,
                                        JSONObject data, boolean sleepIfThrottled)
                                        throws MonkeyLearnException {
        while (true) {
            RestClient client = new RestClient(url);
            if (data != null) {
                client.setJsonString(data.toString());
            }
            client.AddHeader("Authorization", "Token " + this.token);
            client.AddHeader("Content-Type", "application/json");
            try {
                if (method.equals("POST")) {
                    client.Execute(RestClient.RequestMethod.POST);
                } else if (method.equals("GET")) {
                    client.Execute(RestClient.RequestMethod.GET);
                } else if (method.equals("DELETE")) {
                    client.Execute(RestClient.RequestMethod.DELETE);
                } else if (method.equals("PUT")) {
                    client.Execute(RestClient.RequestMethod.PUT);
                } else if (method.equals("PATCH")) {
                    client.Execute(RestClient.RequestMethod.PATCH);
                }
            } catch (Exception e) {
                MonkeyLearnException mle = new MonkeyLearnException(e.getMessage());
                mle.initCause(e);
                throw mle;
            }

            int code = client.getResponseCode();
            String response = client.getResponse();
            Header[] headers = client.getResponseHeaders();

            Object obj = JSONValue.parse(response);
            JSONObject jsonResponse = (JSONObject) obj;

            if (jsonResponse == null)
                throw new MonkeyLearnException("Not received JSON response. HTTP code received " + code);

            if (sleepIfThrottled && code == 429 && jsonResponse.get("detail").toString().contains("seconds")) {
                Pattern pattern = Pattern.compile("available in (\\d+) seconds");
                Matcher matcher = pattern.matcher(jsonResponse.get("detail").toString());
                if (matcher.find()) {
                    int miliseconds = Integer.parseInt(matcher.group(1)) * 1000;
                    try {
                        Thread.sleep(miliseconds);
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
            } else if (sleepIfThrottled && code == 429 && jsonResponse.get("detail").toString().contains("Too many concurrent requests")) {
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                continue;
            } else if (code != 200) {
                Object detail = jsonResponse.get("detail");
                if (detail != null)
                    throw new MonkeyLearnException(detail.toString());
                else
                    throw new MonkeyLearnException(jsonResponse.toString());

            }

            return new Tuple<JSONObject, Header[]>(jsonResponse, headers);
        }
    }
}
