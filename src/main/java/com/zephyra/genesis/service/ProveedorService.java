package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.ProveedorRequest;
import com.zephyra.genesis.dto.ProveedorResponse;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.TIPO_DOCUMENTO;
import com.zephyra.genesis.entity.ProductoEntity;
import com.zephyra.genesis.repository.ProductoRepository;
import com.zephyra.genesis.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public ProveedorService(ProveedorRepository proveedorRepository, ProductoRepository productoRepository) {
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAllByOrderByRazonSocialAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse buscarPorDocumento(String documento) {
        return proveedorRepository.findByNumeroDocumentoIgnoreCase(documento)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> buscarPorDenominacion(String denominacion) {
        return proveedorRepository.findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(denominacion).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        validarObligatorios(request);
        validarDuplicados(null, request);
        ProveedorEntity proveedor = new ProveedorEntity(
                request.name(),
                request.email(),
                request.telefono(),
                request.numeroDocumento(),
                request.direccion(),
                request.razonSocial(),
                parseTipoDocumento(request.tipoDocumento()),
                null
        );
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizarPorDocumento(String documento, ProveedorRequest request) {
        ProveedorEntity proveedor = proveedorRepository.findByNumeroDocumentoIgnoreCase(documento)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        validarObligatorios(request);
        validarDuplicados(proveedor, request);
        proveedor.setName(request.name());
        proveedor.setEmail(request.email());
        proveedor.setTelefono(request.telefono());
        proveedor.setNumeroDocumento(request.numeroDocumento());
        proveedor.setDireccion(request.direccion());
        proveedor.setRazonSocial(request.razonSocial());
        proveedor.setTipoDocumento(parseTipoDocumento(request.tipoDocumento()));
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public void desactivarPorDocumento(String documento) {
        ProveedorEntity proveedor = proveedorRepository.findByNumeroDocumentoIgnoreCase(documento)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        List<ProductoEntity> productos = productoRepository.findByProveedorId_Id(proveedor.getId());
        if (!productos.isEmpty()) {
            productoRepository.deleteAll(productos);
        }
        proveedorRepository.delete(proveedor);
    }

    private TIPO_DOCUMENTO parseTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            return TIPO_DOCUMENTO.CI;
        }
        String normalized = tipoDocumento.trim().toUpperCase();
        return switch (normalized) {
            case "RUT" -> TIPO_DOCUMENTO.RUT;
            case "RUC" -> TIPO_DOCUMENTO.RUC;
            default -> TIPO_DOCUMENTO.CI;
        };
    }

    private void validarObligatorios(ProveedorRequest request) {
        if (request.numeroDocumento() == null || request.numeroDocumento().trim().isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio.");
        }
        if (request.razonSocial() == null || request.razonSocial().trim().isBlank()) {
            throw new IllegalArgumentException("La razón social es obligatoria.");
        }
        if (request.direccion() == null || request.direccion().trim().isBlank()) {
            throw new IllegalArgumentException("La dirección es obligatoria.");
        }
    }

    private void validarDuplicados(ProveedorEntity actual, ProveedorRequest request) {
        String numeroDocumento = request.numeroDocumento() != null ? request.numeroDocumento().trim() : "";
        if (!numeroDocumento.isBlank()) {
            boolean documentoEnUso = proveedorRepository.existsByNumeroDocumentoIgnoreCase(numeroDocumento)
                    && (actual == null || !numeroDocumento.equalsIgnoreCase(actual.getNumeroDocumento()));
            if (documentoEnUso) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese número de documento.");
            }
        }

        String email = request.email() != null ? request.email().trim() : "";
        if (!email.isBlank()) {
            boolean emailEnUso = proveedorRepository.existsByEmailIgnoreCase(email)
                    && (actual == null || actual.getEmail() == null || !email.equalsIgnoreCase(actual.getEmail()));
            if (emailEnUso) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese email.");
            }
        }
    }

    private ProveedorResponse toResponse(ProveedorEntity proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getName(),
                proveedor.getEmail(),
                proveedor.getTelefono(),
                proveedor.getNumeroDocumento(),
                proveedor.getDireccion(),
                proveedor.getRazonSocial(),
                proveedor.getTipoDocumento() != null ? proveedor.getTipoDocumento().name() : null
        );
    }
}
