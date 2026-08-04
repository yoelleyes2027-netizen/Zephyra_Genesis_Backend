package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.EtiquetaResponse;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.repository.ProductoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EtiquetaService {

    private final ProductoRepository productoRepository;
    private final Path storagePath;
    private final Map<Long, String> etiquetas = new LinkedHashMap<>();
    private long nextId = 1L;
    private boolean loaded;

    public EtiquetaService(ProductoRepository productoRepository,
                           @Value("${app.etiquetas.file:./data/etiquetas.txt}") String storageFile) {
        this.productoRepository = productoRepository;
        this.storagePath = Path.of(storageFile);
    }

    @PostConstruct
    void init() {
        ensureLoaded();
    }

    public synchronized List<EtiquetaResponse> listar() {
        ensureLoaded();
        return etiquetas.entrySet().stream()
                .map(entry -> new EtiquetaResponse(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()))
                .toList();
    }

    public synchronized EtiquetaResponse agregar(String nombre) {
        ensureLoaded();
        String normalized = normalize(nombre);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar un nombre de etiqueta.");
        }

        EtiquetaResponse existente = buscarPorNombre(normalized);
        if (existente != null) {
            return existente;
        }

        EtiquetaResponse nueva = new EtiquetaResponse(nextId++, normalized);
        etiquetas.put(nueva.id(), nueva.nombre());
        persistir();
        return nueva;
    }

    public synchronized void registrarDesdeProducto(String nombre) {
        ensureLoaded();
        String normalized = normalize(nombre);
        if (normalized.isBlank()) {
            return;
        }
        if (buscarPorNombre(normalized) == null) {
            etiquetas.put(nextId++, normalized);
            persistir();
        }
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }

        cargarDesdeArchivo();
        cargarDesdeProductos();
        persistir();
        loaded = true;
    }

    private void cargarDesdeArchivo() {
        try {
            if (!Files.exists(storagePath)) {
                Path parent = storagePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                return;
            }

            List<String> lines = Files.readAllLines(storagePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    continue;
                }
                Long id = Long.parseLong(parts[0].trim());
                String nombre = normalize(parts[1]);
                etiquetas.put(id, nombre);
                nextId = Math.max(nextId, id + 1);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron cargar las etiquetas.", ex);
        }
    }

    private void cargarDesdeProductos() {
        for (ProductoEntity producto : productoRepository.findAll()) {
            String etiqueta = normalize(producto.getEtiqueta());
            if (!etiqueta.isBlank() && buscarPorNombre(etiqueta) == null) {
                etiquetas.put(nextId++, etiqueta);
            }
        }
    }

    private EtiquetaResponse buscarPorNombre(String nombre) {
        String normalized = normalize(nombre).toLowerCase(Locale.ROOT);
        return etiquetas.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().toLowerCase(Locale.ROOT).equals(normalized))
                .map(entry -> new EtiquetaResponse(entry.getKey(), entry.getValue()))
                .findFirst()
                .orElse(null);
    }

    private void persistir() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = new ArrayList<>();
            for (Map.Entry<Long, String> entry : etiquetas.entrySet()) {
                lines.add(entry.getKey() + "|" + entry.getValue());
            }

            Files.write(storagePath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudieron guardar las etiquetas.", ex);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
