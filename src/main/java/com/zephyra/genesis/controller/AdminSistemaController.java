package com.zephyra.genesis.controller;

import com.zephyra.genesis.dto.UsuarioAdminRequest;
import com.zephyra.genesis.dto.UsuarioAdminResponse;
import com.zephyra.genesis.service.AdminSistemaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin-sistema")
public class AdminSistemaController {

    private final AdminSistemaService adminSistemaService;

    public AdminSistemaController(AdminSistemaService adminSistemaService) {
        this.adminSistemaService = adminSistemaService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        return ResponseEntity.ok(Map.of("ok", true, "data", adminSistemaService.listarUsuarios()));
    }

    @GetMapping("/bases-datos")
    public ResponseEntity<?> listarBasesDeDatos() {
        return ResponseEntity.ok(Map.of("ok", true, "data", adminSistemaService.listarBasesDeDatos()));
    }

    @GetMapping("/tablas")
    public ResponseEntity<?> listarTablas(@RequestParam String baseDatos) {
        return ResponseEntity.ok(Map.of("ok", true, "data", adminSistemaService.listarTablas(baseDatos)));
    }

    @GetMapping("/usuarios/{cedula}")
    public ResponseEntity<UsuarioAdminResponse> buscarUsuario(@PathVariable int cedula) {
        return ResponseEntity.ok(adminSistemaService.buscarUsuario(cedula));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody UsuarioAdminRequest request) {
        UsuarioAdminResponse usuario = adminSistemaService.crearUsuario(request);
        return ResponseEntity.status(201).body(Map.of("ok", true, "mensaje", "Usuario creado correctamente", "usuario", usuario));
    }

    @PutMapping("/usuarios/{cedula}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable int cedula, @RequestBody UsuarioAdminRequest request) {
        UsuarioAdminResponse usuario = adminSistemaService.actualizarUsuario(cedula, request);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Usuario actualizado correctamente", "usuario", usuario));
    }

    @DeleteMapping("/usuarios/{cedula}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable int cedula) {
        adminSistemaService.eliminarUsuario(cedula);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Usuario eliminado correctamente"));
    }

    @GetMapping("/soporte")
    public ResponseEntity<?> soporte(@RequestParam String baseDatos, @RequestParam String tabla) {
        return ResponseEntity.ok(Map.of("ok", true, "data", adminSistemaService.soporte(baseDatos, tabla)));
    }

    @PutMapping("/soporte")
    public ResponseEntity<?> editarSoporte(@RequestBody Map<String, Object> body) {
        String baseDatos = stringValue(body.get("baseDatos"));
        String tabla = stringValue(body.get("tabla"));
        String clave = stringValue(body.get("clave"));
        Map<String, Object> datos = mapValue(body.get("data"));
        Map<String, Object> resultado = adminSistemaService.editarSoporte(baseDatos, tabla, clave, datos);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Registro actualizado correctamente", "data", resultado));
    }

    @DeleteMapping("/soporte")
    public ResponseEntity<?> eliminarSoporte(@RequestBody Map<String, Object> body) {
        String baseDatos = stringValue(body.get("baseDatos"));
        String tabla = stringValue(body.get("tabla"));
        String clave = stringValue(body.get("clave"));
        adminSistemaService.eliminarSoporte(baseDatos, tabla, clave);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Registro eliminado correctamente"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}