package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.service.CitaService;

import lombok.AllArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@AllArgsConstructor
public class CitaController {

    private final CitaService citaService;

    // Personal: cualquier mascota. Cliente: solo para sus propias mascotas.
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.mascota(authentication, #mascotaId)")
    @PostMapping
    public ResponseEntity<Cita> agendarCita(
            @RequestParam Long mascotaId,
            @RequestParam Long veterinarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam String motivo) {
        Cita nuevaCita = citaService.agendarCita(mascotaId, veterinarioId, fechaHora, motivo);
        return ResponseEntity.ok(nuevaCita);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.cita(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<Cita> obtenerCitaPorId(@PathVariable Long id) {
        return citaService.obtenerCitaPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @PutMapping("/{id}/estado")
    public ResponseEntity<Cita> actualizarEstadoCita(@PathVariable Long id, @RequestParam String estado) {
        Cita citaActualizada = citaService.actualizarEstadoCita(id, estado);
        return ResponseEntity.ok(citaActualizada);
    }

    // El cliente solo puede cancelar las citas de sus mascotas.
    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.cita(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarCita(@PathVariable Long id) {
        citaService.cancelarCita(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE') or @accesoRecursos.mascota(authentication, #mascotaId)")
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<List<Cita>> listarCitasPorMascota(@PathVariable Long mascotaId) {
        List<Cita> citas = citaService.listarCitasPorMascota(mascotaId);
        return ResponseEntity.ok(citas);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @GetMapping("/veterinario/{veterinarioId}")
    public ResponseEntity<List<Cita>> listarCitasPorVeterinario(@PathVariable Long veterinarioId) {
        List<Cita> citas = citaService.listarCitasPorVeterinario(veterinarioId);
        return ResponseEntity.ok(citas);
    }

    @PreAuthorize("hasAnyRole('EMPLEADO','GERENTE')")
    @GetMapping("/entre-fechas")
    public ResponseEntity<List<Cita>> listarCitasEntreFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        List<Cita> citas = citaService.listarCitasEntreFechas(inicio, fin);
        return ResponseEntity.ok(citas);
    }
}
