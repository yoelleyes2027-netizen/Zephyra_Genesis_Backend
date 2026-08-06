package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.EmpresaRequest;
import com.zephyra.genesis.dto.EmpresaResponse;
import com.zephyra.genesis.entity.EmpresaEntity;
import com.zephyra.genesis.entity.TIPO_DOCUMENTO;
import com.zephyra.genesis.repository.EmpresaRepository;
import com.zephyra.genesis.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final TicketRepository ticketRepository;

    public EmpresaService(EmpresaRepository empresaRepository, TicketRepository ticketRepository) {
        this.empresaRepository = empresaRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<EmpresaResponse> listar() {
        return empresaRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EmpresaResponse buscarPorDocumento(String documento) {
        return empresaRepository.findByNumeroDocumentoIgnoreCase(documento)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    public List<EmpresaResponse> buscarPorRazonSocial(String razonSocial) {
        return empresaRepository.findByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(razonSocial).stream().map(this::toResponse).toList();
    }

    public EmpresaResponse crear(EmpresaRequest request) {
        if (empresaRepository.findByNumeroDocumentoIgnoreCase(request.numeroDocumento()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una empresa con ese número de documento.");
        }

        EmpresaEntity empresa = new EmpresaEntity(
                request.name(),
                request.email(),
                request.telefono(),
                request.razonSocial(),
                parseTipoDocumento(request.tipoDocumento()),
                request.direccion(),
                request.numeroDocumento()
        );

        return toResponse(empresaRepository.save(empresa));
    }

    public EmpresaResponse actualizarPorDocumento(String documento, EmpresaRequest request) {
        EmpresaEntity empresa = empresaRepository.findByNumeroDocumentoIgnoreCase(documento)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        empresa.setName(request.name());
        empresa.setEmail(request.email());
        empresa.setTelefono(request.telefono());
        empresa.setRazonSocial(request.razonSocial());
        empresa.setTipoDocumento(parseTipoDocumento(request.tipoDocumento()));
        empresa.setDireccion(request.direccion());
        empresa.setNumeroDocumento(request.numeroDocumento());

        return toResponse(empresaRepository.save(empresa));
    }

    public void eliminarPorDocumento(String documento) {
        EmpresaEntity empresa = empresaRepository.findByNumeroDocumentoIgnoreCase(documento)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        if (ticketRepository.existsByCliente_Id(empresa.getId())) {
            throw new IllegalArgumentException("No se puede eliminar la empresa porque tiene tickets asociados.");
        }
        empresaRepository.delete(empresa);
    }

    private EmpresaResponse toResponse(EmpresaEntity empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getEmail(),
                empresa.getTelefono(),
                empresa.getRazonSocial(),
                empresa.getTipoDocumento() != null ? empresa.getTipoDocumento().name() : null,
                empresa.getDireccion(),
                empresa.getNumeroDocumento(),
                empresa.getFechaCreacion()
        );
    }

    private TIPO_DOCUMENTO parseTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null || tipoDocumento.isBlank()) {
            throw new IllegalArgumentException("Tipo de documento es obligatorio.");
        }
        return TIPO_DOCUMENTO.valueOf(tipoDocumento.trim().toUpperCase());
    }
}