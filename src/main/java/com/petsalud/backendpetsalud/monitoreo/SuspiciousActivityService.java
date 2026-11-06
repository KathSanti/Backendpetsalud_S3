package com.petsalud.backendpetsalud.monitoreo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SuspiciousActivityService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;

    private final Map<String, Integer> intentosFallidosPorIp = new ConcurrentHashMap<>();

    public void registrarLoginFallido(String email, String ip) {
        int intentos = intentosFallidosPorIp.merge(ip, 1, Integer::sum);
        log.warn("[SEGURIDAD] Login fallido: email={} ip={} intentos={}", limpiar(email), ip, intentos);

        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            log.error("[SEGURIDAD][ALERTA] Posible ataque de fuerza bruta desde ip={} ({} intentos fallidos)", ip, intentos);
        }
    }

    public void registrarLoginExitoso(String email, String ip) {
        intentosFallidosPorIp.remove(ip);
        log.info("[SEGURIDAD] Login exitoso: email={} ip={}", limpiar(email), ip);
    }

    public void registrarTokenInvalido(String ip, String ruta, String motivo) {
        log.warn("[SEGURIDAD] Token inválido: ip={} ruta={} motivo={}", ip, limpiar(ruta), limpiar(motivo));
    }

    public void registrarAccesoDenegado(String usuario, String metodo, String ruta, String ip) {
        log.warn("[SEGURIDAD] Acceso denegado: usuario={} {} {} ip={}", usuario, metodo, limpiar(ruta), ip);
    }

    public void registrarPatronSospechoso(String ip, String metodo, String ruta, String valor) {
        log.warn("[SEGURIDAD] Patrón sospechoso: ip={} {} {} valor={}", ip, metodo, limpiar(ruta), limpiar(valor));
    }

    // Evita que un atacante falsifique líneas del log con saltos de línea
    private String limpiar(String texto) {
        if (texto == null) {
            return "null";
        }
        String limpio = texto.replaceAll("[\\r\\n\\t]", "_");
        return limpio.length() > 100 ? limpio.substring(0, 100) + "..." : limpio;
    }
}