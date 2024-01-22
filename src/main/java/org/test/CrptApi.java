package org.test;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;
    private int requestCount;
    private Instant lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = 0;
        this.lastRequestTime = Instant.now();
    }

    /***
     * Для проверки
     */
    public static void main(String[] args)  {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 2);
        String json = "{\"description\":{\"participantInn\":\"1234567890\"},\"doc_id\":\"document123\",\"doc_status\":\"draft\",\"doc_type\":\"LP_INTRODUCE_GOODS\",\"importRequest\":true,\"owner_inn\":\"0987654321\",\"participant_inn\":\"1234567890\",\"producer_inn\":\"5555555555\",\"production_date\":\"2020-01-23\",\"production_type\":\"raw_material\",\"products\":[{\"certificate_document\":\"cert123\",\"certificate_document_date\":\"2020-01-23\"}],\"reg_date\":\"2020-01-23\",\"reg_number\":\"reg001\"}";

        try {
            for (int i = 0; i < 3; i++) {
                crptApi.createDocument(json);
                System.out.println("Запрос успешно отправлен!");
            }

        } catch (IOException e) {
            System.err.println("Ошибка при отправке запроса: " + e.getMessage());
        }
    }

    public void createDocument(String document) throws IOException {
        throttleRequests();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
        StringEntity entity = new StringEntity(document);
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(httpPost)) {
            System.out.println(response);
        }
    }

    private void throttleRequests() {
        Instant now = Instant.now();
        long timeElapsed = timeUnit.convert(now.toEpochMilli() - lastRequestTime.toEpochMilli(), TimeUnit.MILLISECONDS);
        if (timeElapsed >= timeUnit.toSeconds(1)) {
            requestCount = 0;
            lastRequestTime = now;
        }
        if (requestCount >= requestLimit) {
            try {
                long sleepTime = Math.max(0, timeUnit.toMillis(1) - (now.toEpochMilli() - lastRequestTime.toEpochMilli()));
                System.out.println("Ждем " + sleepTime);
                Thread.sleep(sleepTime);
                requestCount = 0;
                lastRequestTime = Instant.now();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        requestCount++;
    }
}