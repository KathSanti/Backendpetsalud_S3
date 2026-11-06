package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba la propiedad de los recursos usando datos persistidos, nunca el cuerpo de la solicitud. */
@Component("accesoRecursos")
@Transactional(readOnly = true)
public class AccesoRecursos {
    private final MascotaRepository mascotas;
    private final CitaRepository citas;
    private final ExpedienteMedicoRepository expedientes;
    private final VentaRepository ventas;
    private final RecompensaRepository recompensas;
    private final NotificacionRepository notificaciones;

    public AccesoRecursos(MascotaRepository mascotas, CitaRepository citas,
            ExpedienteMedicoRepository expedientes, VentaRepository ventas,
            RecompensaRepository recompensas, NotificacionRepository notificaciones) {
        this.mascotas = mascotas;
        this.citas = citas;
        this.expedientes = expedientes;
        this.ventas = ventas;
        this.recompensas = recompensas;
        this.notificaciones = notificaciones;
    }

    public boolean usuario(Authentication authentication, Long usuarioId) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UsuarioDetails details
                && usuarioId != null && usuarioId.equals(details.getId());
    }

    private boolean propietario(Authentication authentication, Mascota mascota) {
        return mascota != null && pertenece(authentication, mascota.getPropietario());
    }

    private boolean pertenece(Authentication authentication, Usuario usuario) {
        return usuario != null && usuario(authentication, usuario.getId());
    }

    public boolean mascota(Authentication authentication, Long id) {
        return id != null && mascotas.findById(id).map(m -> propietario(authentication, m)).orElse(false);
    }

    public boolean cita(Authentication authentication, Long id) {
        return id != null && citas.findById(id).map(c -> propietario(authentication, c.getMascota())).orElse(false);
    }

    public boolean expediente(Authentication authentication, Long id) {
        return id != null && expedientes.findById(id)
                .map(e -> propietario(authentication, e.getMascota())).orElse(false);
    }

    public boolean venta(Authentication authentication, Long id) {
        return id != null && ventas.findById(id).map(v -> pertenece(authentication, v.getUsuario())).orElse(false);
    }

    public boolean recompensa(Authentication authentication, Long id) {
        return id != null && recompensas.findById(id).map(r -> pertenece(authentication, r.getUsuario())).orElse(false);
    }

    public boolean notificacion(Authentication authentication, Long id) {
        return id != null && notificaciones.findById(id).map(n -> pertenece(authentication, n.getUsuario())).orElse(false);
    }
}
