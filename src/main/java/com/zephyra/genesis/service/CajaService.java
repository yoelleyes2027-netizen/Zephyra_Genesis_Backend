package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.CerrarCajaRequest;
import com.zephyra.genesis.entity.CajaDiariaEntity;
import com.zephyra.genesis.entity.CajaGlobalEntity;
import com.zephyra.genesis.entity.FORMA_DE_PAGO;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.UsuarioEntity;
import com.zephyra.genesis.repository.CajaDiariaRepository;
import com.zephyra.genesis.repository.CajaGlobalRepository;
import com.zephyra.genesis.repository.TicketRepository;
import com.zephyra.genesis.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class CajaService {

    private final CajaGlobalRepository cajaGlobalRepository;
    private final CajaDiariaRepository cajaDiariaRepository;
    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CajaService(
            CajaGlobalRepository cajaGlobalRepository,
            CajaDiariaRepository cajaDiariaRepository,
            TicketRepository ticketRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.cajaGlobalRepository = cajaGlobalRepository;
        this.cajaDiariaRepository = cajaDiariaRepository;
        this.ticketRepository = ticketRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CajaGlobalEntity iniciarDia(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN) {
            throw new IllegalArgumentException("Solo un admin de la BDD puede iniciar el día.");
        }

        CajaGlobalEntity cajaGlobal = new CajaGlobalEntity(new Date());
        return cajaGlobalRepository.save(cajaGlobal);
    }

    @Transactional
    public CajaDiariaEntity cerrarCaja(Long usuarioId, CerrarCajaRequest request) {
        validarRequestCierre(request);

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.CAJERO) {
            throw new IllegalArgumentException("Solo usuarios admin o cajero pueden cerrar caja.");
        }

        validarAutorizacionAdmin(request.cedulaAdmin(), request.contrasenaAdmin());

        CajaGlobalEntity cajaGlobalActual = cajaGlobalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("Primero debes iniciar el día."));

        Date fechaInicio = cajaGlobalActual.getFechaInicio();
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La caja global actual no tiene fecha de inicio.");
        }

        CajaDiariaEntity cajaDiaria = cajaDiariaRepository.findByUsuario_Id(usuarioId)
                .orElseGet(CajaDiariaEntity::new);

        if (cajaDiaria.getCajaGlobal() != null
                && cajaDiaria.getCajaGlobal().getId() != null
                && cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())
                && cajaDiaria.getFechaCierre() != null) {
            throw new IllegalArgumentException("La caja de este usuario ya fue cerrada para el día actual.");
        }

        float transferenciaCalculada = valor(ticketRepository.sumarMontoPorUsuarioDesdeYFormaDePago(
                usuarioId,
                fechaInicio,
                FORMA_DE_PAGO.TRANSFERENCIA));
        float posCalculado = valor(ticketRepository.sumarMontoPorUsuarioDesdeYFormaDePago(
                usuarioId,
                fechaInicio,
                FORMA_DE_PAGO.TARJETA));
        float efectivoCalculado = valor(ticketRepository.sumarMontoPorUsuarioDesdeYFormaDePago(
                usuarioId,
                fechaInicio,
                FORMA_DE_PAGO.EFECTIVO));
        float totalIngresos = valor(ticketRepository.sumarIngresosPorUsuarioDesde(usuarioId, fechaInicio));
        float totalEgresos = valor(ticketRepository.sumarEgresosPorUsuarioDesde(usuarioId, fechaInicio));

        float posDeclarado = request.posDeclarado();
        float efectivoDeclarado = request.efectivoDeclarado();
        float diferenciaPos = redondear(posDeclarado - posCalculado);
        float diferenciaEfectivo = redondear(efectivoDeclarado - efectivoCalculado);

        cajaDiaria.setUsuario(usuario);
        cajaDiaria.setCajaGlobal(cajaGlobalActual);
        cajaDiaria.setFechaInicio(fechaInicio);
        cajaDiaria.setFechaCierre(new Date());
        cajaDiaria.setTransferenciaCalculada(redondear(transferenciaCalculada));
        cajaDiaria.setPosCalculado(redondear(posCalculado));
        cajaDiaria.setPosDeclarado(redondear(posDeclarado));
        cajaDiaria.setEfectivoCalculado(Math.round(efectivoCalculado));
        cajaDiaria.setEfectivoDeclarado(Math.round(efectivoDeclarado));
        cajaDiaria.setDiferenciaPos(diferenciaPos);
        cajaDiaria.setDiferenciaEfectivo(diferenciaEfectivo);
        cajaDiaria.setTotalIngresos(redondear(totalIngresos));
        cajaDiaria.setTotalEgresos(redondear(totalEgresos));

        return cajaDiariaRepository.save(cajaDiaria);
    }

    @Transactional
    public CajaGlobalEntity cerrarDia(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN) {
            throw new IllegalArgumentException("Solo un admin de la BDD puede cerrar el día.");
        }

        CajaGlobalEntity cajaGlobalActual = cajaGlobalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("Primero debes iniciar el día."));

        if (cajaGlobalActual.getFechaCierre() != null) {
            throw new IllegalArgumentException("El día actual ya está cerrado.");
        }

        List<Object[]> filasTotales = cajaDiariaRepository.obtenerTotalesCierreDia();
        Object[] totales = (filasTotales == null || filasTotales.isEmpty() || filasTotales.get(0) == null)
            ? new Object[0]
            : filasTotales.get(0);
        cajaGlobalActual.setTotalIngresos(aFloat(totales, 0));
        cajaGlobalActual.setTotalEgresos(aFloat(totales, 1));
        cajaGlobalActual.setTransferenciaCalculada(aFloat(totales, 2));
        cajaGlobalActual.setPosCalculado(aFloat(totales, 3));
        cajaGlobalActual.setPosDeclarado(aFloat(totales, 4));
        cajaGlobalActual.setDiferenciaPos(aFloat(totales, 5));
        cajaGlobalActual.setDiferenciaEfectivo(aFloat(totales, 6));
        cajaGlobalActual.setEfectivoCalculado(aInteger(totales, 7));
        cajaGlobalActual.setEfectivoDeclarado(aInteger(totales, 8));
        cajaGlobalActual.setFechaCierre(new Date());

        CajaGlobalEntity cajaGlobalCerrada = cajaGlobalRepository.save(cajaGlobalActual);
        cajaDiariaRepository.truncateCajaDiaria();
        return cajaGlobalCerrada;
    }

    @Transactional(readOnly = true)
    public void validarCierreParaLogout(AuthUserResponse usuarioToken) {
        if (usuarioToken == null) {
            return;
        }

        String rol = usuarioToken.rol() == null ? "" : usuarioToken.rol().trim().toLowerCase();
        if (!"admin".equals(rol) && !"cajero".equals(rol)) {
            return;
        }

        CajaGlobalEntity cajaGlobalActual = cajaGlobalRepository.findTopByOrderByIdDesc().orElse(null);
        if (cajaGlobalActual == null) {
            return;
        }
        if (cajaGlobalActual.getFechaCierre() != null) {
            return;
        }

        CajaDiariaEntity cajaDiaria = cajaDiariaRepository.findByUsuario_Id(usuarioToken.id()).orElse(null);
        boolean cajaCerrada = cajaDiaria != null
                && cajaDiaria.getCajaGlobal() != null
                && cajaDiaria.getCajaGlobal().getId() != null
                && cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())
                && cajaDiaria.getFechaCierre() != null;

        if (!cajaCerrada) {
            throw new IllegalStateException("Debes cerrar caja antes de cerrar sesión.");
        }
    }

    @Transactional(readOnly = true)
    public void validarAccesoCajas(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.CAJERO) {
            throw new IllegalArgumentException("Solo admin o cajero pueden acceder al módulo Cajas.");
        }

        CajaGlobalEntity ultimaCajaGlobal = cajaGlobalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalStateException("La caja global no ha sido iniciada"));

        boolean soloInicio = ultimaCajaGlobal.getFechaInicio() != null
                && ultimaCajaGlobal.getFechaCierre() == null
                && ultimaCajaGlobal.getTotalIngresos() == null
                && ultimaCajaGlobal.getTotalEgresos() == null
                && ultimaCajaGlobal.getTransferenciaCalculada() == null
                && ultimaCajaGlobal.getDiferenciaPos() == null
                && ultimaCajaGlobal.getDiferenciaEfectivo() == null
                && ultimaCajaGlobal.getPosCalculado() == null
                && ultimaCajaGlobal.getPosDeclarado() == null
                && ultimaCajaGlobal.getEfectivoCalculado() == null
                && ultimaCajaGlobal.getEfectivoDeclarado() == null;

        if (!soloInicio) {
            throw new IllegalStateException("La caja global no ha sido iniciada");
        }
    }

    private void validarAutorizacionAdmin(Integer cedulaAdmin, String contrasenaAdmin) {
        if (cedulaAdmin == null || contrasenaAdmin == null || contrasenaAdmin.isBlank()) {
            throw new IllegalArgumentException("Cédula y contraseña de administrador son obligatorias.");
        }

        UsuarioEntity admin = usuarioRepository.findByCedula(cedulaAdmin)
                .orElseThrow(() -> new IllegalArgumentException("Autorización denegada."));
        if (admin.getRol() != ROL.ADMIN || !passwordEncoder.matches(contrasenaAdmin, admin.getPassword())) {
            throw new IllegalArgumentException("Autorización denegada.");
        }
    }

    private void validarRequestCierre(CerrarCajaRequest request) {
        if (request == null
                || request.posDeclarado() == null
                || request.efectivoDeclarado() == null
                || request.posDeclarado() < 0
                || request.efectivoDeclarado() < 0) {
            throw new IllegalArgumentException("Debes ingresar pos declarado y efectivo declarado válidos.");
        }
    }

    private float valor(Float number) {
        return number == null ? 0f : number;
    }

    private float redondear(float monto) {
        return Math.round(monto * 100f) / 100f;
    }

    private float aFloat(Object[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return 0f;
        }
        Object value = values[index];
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    private int aInteger(Object[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return 0;
        }
        Object value = values[index];
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Math.round(Float.parseFloat(value.toString()));
    }
}
