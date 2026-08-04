package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.ProveedorRequest;
import com.zephyra.genesis.dto.ProveedorResponse;
import com.zephyra.genesis.entity.ProveedorEntity;
import com.zephyra.genesis.entity.TIPO_DOCUMENTO;
import com.zephyra.genesis.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository.findAll().stream().map(this::toResponse).toList();
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
