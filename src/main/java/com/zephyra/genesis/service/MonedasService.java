package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.MonedaResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class MonedasService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public MonedaResponse obtenerDolar() {
        return new MonedaResponse(true, "USD", "Dólar", obtenerValorUsdUYU());
    }

    public double obtenerValorUsdUYU() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://open.er-api.com/v6/latest/USD"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("No se pudo consultar la API de monedas.");
            }

            Map<?, ?> payload = new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body(), Map.class);
            Map<?, ?> rates = (Map<?, ?>) payload.get("rates");
            Object uyu = rates != null ? rates.get("UYU") : null;
            if (uyu == null) {
                throw new IllegalStateException("No se encontró la cotización UYU.");
            }

            return Double.parseDouble(uyu.toString());
        } catch (IOException ex) {
            throw new IllegalStateException("Error al consultar la API de monedas.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Error al consultar la API de monedas.", ex);
        }
    }
}
