package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.ClienteRequest;
import com.zephyra.genesis.dto.ClienteResponse;
import com.zephyra.genesis.entity.ClienteEntity;
import com.zephyra.genesis.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorEmail(String email) {
        return clienteRepository.findByEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNombre(String nombre) {
        return clienteRepository.findByNameContainingIgnoreCaseOrderByNameAsc(nombre).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        ClienteEntity cliente = new ClienteEntity(request.name(), request.email(), request.telefono());
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizarPorEmail(String email, ClienteRequest request) {
        ClienteEntity cliente = clienteRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        cliente.setName(request.name());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void desactivarPorEmail(String email) {
        ClienteEntity cliente = clienteRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        clienteRepository.delete(cliente);
    }

    private ClienteResponse toResponse(ClienteEntity cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getName(), cliente.getEmail(), cliente.getTelefono(), cliente.getFechaCreacion());
    }
}
