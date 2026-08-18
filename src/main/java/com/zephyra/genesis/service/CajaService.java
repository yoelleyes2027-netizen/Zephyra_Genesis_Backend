package com.zephyra.genesis.service;

import com.zephyra.genesis.dto.AuthUserResponse;
import com.zephyra.genesis.dto.CerrarCajaRequest;
import com.zephyra.genesis.entity.CajaDiariaEntity;
import com.zephyra.genesis.entity.CajaGlobalEntity;
import com.zephyra.genesis.entity.FORMA_DE_PAGO;
import com.zephyra.genesis.entity.ROL;
import com.zephyra.genesis.entity.TIPO_MONEDA;
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
    public CajaDiariaEntity abrirCaja(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.CAJERO) {
            throw new IllegalArgumentException("Solo usuarios admin o cajero pueden abrir caja.");
        }

        CajaGlobalEntity cajaGlobalActual = cajaGlobalRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new IllegalArgumentException("La caja global no ha sido iniciada."));

        CajaDiariaEntity cajaDiaria = cajaDiariaRepository.findByUsuario_Id(usuarioId)
                .orElseGet(CajaDiariaEntity::new);

        if (cajaDiaria.getFechaInicio() != null
                && cajaDiaria.getFechaCierre() == null
                && cajaDiaria.getCajaGlobal() != null
                && cajaDiaria.getCajaGlobal().getId() != null
                && cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())) {
            throw new IllegalArgumentException("La caja ya está abierta para este usuario.");
        }

        cajaDiaria.setUsuario(usuario);
        cajaDiaria.setCajaGlobal(cajaGlobalActual);
        cajaDiaria.setFechaInicio(new Date());
        cajaDiaria.setFechaCierre(null);
        cajaDiaria.setTransferenciaCalculada(0f);
        cajaDiaria.setPosCalculado(0f);
        cajaDiaria.setPosDeclarado(0f);
        cajaDiaria.setEfectivoCalculado(0);
        cajaDiaria.setEfectivoDeclarado(0);
        cajaDiaria.setDolaresCalculados(0f);
        cajaDiaria.setDolaresDeclarados(0f);
        cajaDiaria.setDiferenciaPos(0f);
        cajaDiaria.setDiferenciaEfectivo(0f);
        cajaDiaria.setDiferenciaDolares(0f);
        cajaDiaria.setTotalIngresos(0f);
        cajaDiaria.setTotalEgresos(0f);
        return cajaDiariaRepository.save(cajaDiaria);
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

        CajaDiariaEntity cajaDiaria = cajaDiariaRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Primero debes abrir caja."));

        if (cajaDiaria.getCajaGlobal() == null
                || cajaDiaria.getCajaGlobal().getId() == null
                || !cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())) {
            throw new IllegalArgumentException("Debes abrir caja para el día actual.");
        }

        if (cajaDiaria.getFechaInicio() == null) {
            throw new IllegalArgumentException("Primero debes abrir caja.");
        }

        Date fechaInicio = cajaGlobalActual.getFechaInicio();
        if (fechaInicio == null) {
            throw new IllegalArgumentException("Primero debes iniciar el día.");
        }

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
        float efectivoCalculado = valor(ticketRepository.sumarMontoTotalPorUsuarioDesdeYFormaDePagoYMoneda(
                usuarioId,
                fechaInicio,
            FORMA_DE_PAGO.EFECTIVO,
            TIPO_MONEDA.UYU));
        float dolaresCalculados = valor(ticketRepository.sumarMontoPagadoPorUsuarioDesdeYFormaDePagoYMoneda(
            usuarioId,
            fechaInicio,
            FORMA_DE_PAGO.EFECTIVO,
            TIPO_MONEDA.USD));
        float totalIngresos = valor(ticketRepository.sumarIngresosPorUsuarioDesde(usuarioId, fechaInicio));
        float totalEgresos = valor(ticketRepository.sumarEgresosPorUsuarioDesde(usuarioId, fechaInicio));

        float posDeclarado = request.posDeclarado();
        float efectivoDeclarado = request.efectivoDeclarado();
        float dolaresDeclarados = request.dolaresDeclarados();
        float diferenciaPos = redondear(posDeclarado - posCalculado);
        float diferenciaEfectivo = redondear(efectivoDeclarado - efectivoCalculado);
        float diferenciaDolares = redondear(dolaresDeclarados - dolaresCalculados);

        cajaDiaria.setUsuario(usuario);
        cajaDiaria.setCajaGlobal(cajaGlobalActual);
        cajaDiaria.setFechaCierre(new Date());
        cajaDiaria.setTransferenciaCalculada(redondear(transferenciaCalculada));
        cajaDiaria.setPosCalculado(redondear(posCalculado));
        cajaDiaria.setPosDeclarado(redondear(posDeclarado));
        cajaDiaria.setEfectivoCalculado(Math.round(efectivoCalculado));
        cajaDiaria.setEfectivoDeclarado(Math.round(efectivoDeclarado));
        cajaDiaria.setDolaresCalculados(redondear(dolaresCalculados));
        cajaDiaria.setDolaresDeclarados(redondear(dolaresDeclarados));
        cajaDiaria.setDiferenciaPos(diferenciaPos);
        cajaDiaria.setDiferenciaEfectivo(diferenciaEfectivo);
        cajaDiaria.setDiferenciaDolares(diferenciaDolares);
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
        cajaGlobalActual.setDiferenciaDolares(aFloat(totales, 7));
        cajaGlobalActual.setEfectivoCalculado(aInteger(totales, 8));
        cajaGlobalActual.setEfectivoDeclarado(aInteger(totales, 9));
        cajaGlobalActual.setDolaresDeclarados(aFloat(totales, 10));
        cajaGlobalActual.setDolaresCalculados(aFloat(totales, 11));
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
        if (cajaDiaria == null
            || cajaDiaria.getCajaGlobal() == null
            || cajaDiaria.getCajaGlobal().getId() == null
            || !cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())
            || cajaDiaria.getFechaInicio() == null) {
            return;
        }

        boolean cajaCerrada = cajaDiaria.getFechaCierre() != null;

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
                && ultimaCajaGlobal.getDiferenciaDolares() == null
                && ultimaCajaGlobal.getPosCalculado() == null
                && ultimaCajaGlobal.getPosDeclarado() == null
                && ultimaCajaGlobal.getEfectivoCalculado() == null
                && ultimaCajaGlobal.getEfectivoDeclarado() == null
                && ultimaCajaGlobal.getDolaresDeclarados() == null
                && ultimaCajaGlobal.getDolaresCalculados() == null;

        if (!soloInicio) {
            throw new IllegalStateException("La caja global no ha sido iniciada");
        }
    }

    @Transactional(readOnly = true)
    public boolean cajaAbiertaParaUsuario(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (usuario.getRol() != ROL.ADMIN && usuario.getRol() != ROL.CAJERO) {
            throw new IllegalArgumentException("Solo admin o cajero pueden acceder al módulo Cajas.");
        }

        CajaGlobalEntity cajaGlobalActual = cajaGlobalRepository.findTopByOrderByIdDesc().orElse(null);
        if (cajaGlobalActual == null) {
            return false;
        }

        CajaDiariaEntity cajaDiaria = cajaDiariaRepository.findByUsuario_Id(usuarioId).orElse(null);
        return cajaDiaria != null
                && cajaDiaria.getCajaGlobal() != null
                && cajaDiaria.getCajaGlobal().getId() != null
                && cajaDiaria.getCajaGlobal().getId().equals(cajaGlobalActual.getId())
                && cajaDiaria.getFechaInicio() != null
                && cajaDiaria.getFechaCierre() == null;
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
                || request.dolaresDeclarados() == null
                || request.posDeclarado() < 0
                || request.efectivoDeclarado() < 0
                || request.dolaresDeclarados() < 0) {
            throw new IllegalArgumentException("Debes ingresar pos, efectivo y dolares declarados válidos.");
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
