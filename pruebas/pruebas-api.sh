#!/usr/bin/env bash
# =============================================================================
#  Pruebas de seguridad de la API Clínica PetSalud (curl)
#
#  Requisitos: la aplicación corriendo y los usuarios de prueba creados por
#  DataInitializer (empleado@petsalud.test y gerente@petsalud.test).
#
#  Uso (desde la raíz del proyecto):
#      ./pruebas/pruebas-api.sh
#
#  La contraseña de los usuarios de prueba se toma de la variable SEED_PASSWORD,
#  del archivo .env o, si no está, se pide por teclado (no se muestra).
# =============================================================================
set -u

cd "$(dirname "$0")/.." || exit 1
BASE_URL="${BASE_URL:-http://localhost:8080}"

# ---------- Colores (solo si la salida es una terminal) ----------
if [ -t 1 ]; then
    VERDE=$'\033[32m'; ROJO=$'\033[31m'; AZUL=$'\033[1;34m'; GRIS=$'\033[90m'; NC=$'\033[0m'
else
    VERDE=""; ROJO=""; AZUL=""; GRIS=""; NC=""
fi

# ---------- Contraseña de los usuarios de prueba ----------
if [ -z "${SEED_PASSWORD:-}" ] && [ -f .env ]; then
    SEED_PASSWORD=$(grep -E '^SEED_PASSWORD=' .env | head -1 | cut -d= -f2- | tr -d '\r' | sed -e 's/^"//' -e 's/"$//')
fi
if [ -z "${SEED_PASSWORD:-}" ]; then
    printf "Contraseña de los usuarios de prueba (SEED_PASSWORD): "
    read -rs SEED_PASSWORD
    echo
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
CODIGO=""; CUERPO=""; PASAN=0; FALLAN=0

# ---------- Utilidades ----------

# peticion METODO RUTA [TOKEN] [JSON]  -> deja $CODIGO, $CUERPO y las cabeceras en $TMP/cabeceras
peticion() {
    local metodo=$1 ruta=$2 token=${3:-} datos=${4:-}
    local args=(-s -o "$TMP/cuerpo" -D "$TMP/cabeceras" -w '%{http_code}' -X "$metodo")
    if [ -n "$token" ]; then args+=(-H "Authorization: Bearer $token"); fi
    if [ -n "$datos" ]; then args+=(-H "Content-Type: application/json" --data "$datos"); fi
    CODIGO=$(curl "${args[@]}" "$BASE_URL$ruta")
    CUERPO=$(cat "$TMP/cuerpo")
}

pasa()  { PASAN=$((PASAN + 1));  printf "  %s✔ PASA%s   %s\n" "$VERDE" "$NC" "$1"; }
falla() { FALLAN=$((FALLAN + 1)); printf "  %s✘ FALLA%s  %s\n" "$ROJO" "$NC" "$1"; printf "           %s%s%s\n" "$GRIS" "$2" "$NC"; }

# esperar "descripción" CODIGO_ESPERADO
esperar() {
    if [ "$CODIGO" = "$2" ]; then
        pasa "[$CODIGO] $1"
    else
        falla "[$CODIGO] $1" "se esperaba $2 · respuesta: $(printf '%s' "$CUERPO" | head -c 160)"
    fi
}

# contiene "descripción" "texto"  /  no_contiene "descripción" "texto"
contiene() {
    if printf '%s' "$CUERPO" | grep -qF -- "$2"; then pasa "$1"; else falla "$1" "no se encontró: $2"; fi
}
no_contiene() {
    if printf '%s' "$CUERPO" | grep -qF -- "$2"; then falla "$1" "se encontró: $2"; else pasa "$1"; fi
}
cabecera() {
    if grep -qi -- "$2" "$TMP/cabeceras"; then pasa "$1"; else falla "$1" "falta la cabecera: $2"; fi
}
sin_cabecera() {
    if grep -qi -- "$2" "$TMP/cabeceras"; then falla "$1" "apareció la cabecera: $2"; else pasa "$1"; fi
}

# Extraen el primer valor de un campo JSON del último cuerpo recibido
campo_num() { printf '%s' "$CUERPO" | grep -o "\"$1\":[0-9]*" | head -1 | cut -d: -f2; }
campo_txt() { printf '%s' "$CUERPO" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }

seccion() { printf "\n%s== %s ==%s\n" "$AZUL" "$1" "$NC"; }

login() { peticion POST /api/auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}"; }

# ---------- Comprobar que la API está levantada ----------
if [ "$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/productos")" = "000" ]; then
    printf "%sNo hay respuesta en %s. ¿Está corriendo la aplicación?%s\n" "$ROJO" "$BASE_URL" "$NC"
    exit 1
fi

SUFIJO=$(date +%s)
EMAIL_A="cliente.a.$SUFIJO@petsalud.test"
EMAIL_B="cliente.b.$SUFIJO@petsalud.test"
PASS_CLIENTES="Clave$SUFIJO"

# =============================================================================
seccion "1. Autenticación con JWT"
# =============================================================================
login gerente@petsalud.test "$SEED_PASSWORD"
esperar "Login del gerente con credenciales correctas" 200
TOKEN_GERENTE=$(campo_txt token)
contiene "La respuesta indica el tipo de token Bearer" '"type":"Bearer"'
contiene "La respuesta incluye el rol ROLE_GERENTE" '"role":"ROLE_GERENTE"'
sin_cabecera "Sin cookie de sesión: la API es stateless" "^set-cookie"

login empleado@petsalud.test "$SEED_PASSWORD"
esperar "Login del empleado" 200
TOKEN_EMPLEADO=$(campo_txt token)

login gerente@petsalud.test "ClaveIncorrecta123"
esperar "Login con contraseña incorrecta" 401
MENSAJE_1=$(campo_txt mensaje)

login noexiste@petsalud.test "ClaveIncorrecta123"
esperar "Login con un email que no existe" 401
if [ "$MENSAJE_1" = "$(campo_txt mensaje)" ]; then
    pasa "Mismo mensaje en ambos casos: no revela qué emails existen"
else
    falla "Mismo mensaje en ambos casos" "los mensajes son distintos"
fi

login "esto-no-es-un-email" "Clave1234"
esperar "Login con formato de email inválido (validación)" 400

peticion GET /api/productos
esperar "Endpoint protegido sin token" 401

peticion GET /api/productos "abc.def.ghi"
esperar "Token inventado" 401

# Token alterado: se cambia el contenido para decir ROLE_GERENTE, conservando la firma original
CABECERA_JWT=$(printf '%s' "$TOKEN_EMPLEADO" | cut -d. -f1)
FIRMA_JWT=$(printf '%s' "$TOKEN_EMPLEADO" | cut -d. -f3)
PAYLOAD_FALSO=$(printf '%s' '{"iss":"backendpetsalud","sub":"empleado@petsalud.test","roles":["ROLE_GERENTE"],"exp":4102444800}' \
    | base64 | tr '+/' '-_' | tr -d '=\n')
peticion GET /api/usuarios "$CABECERA_JWT.$PAYLOAD_FALSO.$FIRMA_JWT"
esperar "Token alterado para escalar a GERENTE (firma inválida)" 401

# =============================================================================
seccion "2. Registro de usuarios"
# =============================================================================
peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"Ana\",\"apellido\":\"Soto\",\"email\":\"$EMAIL_A\",\"telefono\":\"+56911111111\",\"password\":\"$PASS_CLIENTES\",\"direccion\":\"Av. Uno 123\",\"rol\":\"GERENTE\"}"
esperar "Registro público de un cliente" 201
CLIENTE_A_ID=$(campo_num id)
contiene "Aunque se envió \"rol\":\"GERENTE\", queda como CLIENTE" '"rol":"CLIENTE"'
no_contiene "La respuesta no incluye la contraseña" '"password"'

peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"Bruno\",\"apellido\":\"Rojas\",\"email\":\"$EMAIL_B\",\"telefono\":\"+56922222222\",\"password\":\"$PASS_CLIENTES\",\"direccion\":\"Calle Dos 456\"}"
esperar "Registro de un segundo cliente" 201
CLIENTE_B_ID=$(campo_num id)

peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"Ana\",\"apellido\":\"Soto\",\"email\":\"$EMAIL_A\",\"password\":\"$PASS_CLIENTES\",\"direccion\":\"Av. Uno 123\"}"
esperar "Registro con un email ya existente" 400

peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"Ana\",\"apellido\":\"Soto\",\"email\":\"debil.$SUFIJO@petsalud.test\",\"password\":\"123\",\"direccion\":\"Av. Uno 123\"}"
esperar "Registro con contraseña de menos de 8 caracteres" 400

login "$EMAIL_A" "$PASS_CLIENTES"; TOKEN_A=$(campo_txt token)
login "$EMAIL_B" "$PASS_CLIENTES"; TOKEN_B=$(campo_txt token)

# =============================================================================
seccion "3. Autorización por rol"
# =============================================================================
peticion GET /api/productos "$TOKEN_A"
esperar "Cliente ve el catálogo de productos" 200

peticion POST /api/productos "$TOKEN_A" '{"nombre":"Collar","precio":5990,"stock":10,"categoria":"Accesorios"}'
esperar "Cliente intenta crear un producto" 403

peticion POST /api/productos "$TOKEN_EMPLEADO" '{"nombre":"Collar de prueba","precio":5990,"stock":10,"categoria":"Accesorios"}'
esperar "Empleado crea un producto" 200
PRODUCTO_ID=$(campo_num id)

peticion DELETE "/api/productos/$PRODUCTO_ID" "$TOKEN_A"
esperar "Cliente intenta eliminar un producto" 403

peticion DELETE "/api/productos/$PRODUCTO_ID" "$TOKEN_EMPLEADO"
esperar "Empleado intenta eliminar un producto" 403

peticion DELETE "/api/productos/$PRODUCTO_ID" "$TOKEN_GERENTE"
esperar "Gerente elimina el producto" 204

peticion GET /api/usuarios "$TOKEN_A"
esperar "Cliente intenta listar todos los usuarios" 403

peticion GET /api/usuarios "$TOKEN_EMPLEADO"
esperar "Empleado intenta listar todos los usuarios" 403

peticion GET /api/usuarios "$TOKEN_GERENTE"
esperar "Gerente lista los usuarios" 200
no_contiene "El listado no expone contraseñas" '"password"'
EMPLEADO_ID=$(printf '%s' "$CUERPO" | grep -o '{[^}]*"email":"empleado@petsalud.test"[^}]*}' | grep -o '"id":[0-9]*' | cut -d: -f2)

peticion GET /api/ventas "$TOKEN_A"
esperar "Cliente intenta ver todas las ventas" 403

# =============================================================================
seccion "4. Propiedad de los recursos (IDOR)"
# =============================================================================
peticion POST "/api/mascotas/propietario/$CLIENTE_A_ID" "$TOKEN_A" '{"nombre":"Firulais","especie":"Perro","raza":"Mestizo","sexo":"Macho"}'
esperar "Cliente A registra su propia mascota" 200
MASCOTA_A_ID=$(campo_num id)
no_contiene "La mascota no expone la contraseña del dueño" '"password"'

peticion POST "/api/mascotas/propietario/$CLIENTE_B_ID" "$TOKEN_A" '{"nombre":"Intruso","especie":"Gato"}'
esperar "Cliente A intenta registrar una mascota a nombre de B" 403

peticion GET "/api/mascotas/$MASCOTA_A_ID" "$TOKEN_A"
esperar "Cliente A ve su mascota" 200

peticion GET "/api/mascotas/$MASCOTA_A_ID" "$TOKEN_B"
esperar "Cliente B intenta ver la mascota de A" 403

peticion PUT "/api/mascotas/$MASCOTA_A_ID" "$TOKEN_B" '{"nombre":"Robado","especie":"Perro"}'
esperar "Cliente B intenta modificar la mascota de A" 403

peticion GET "/api/mascotas/$MASCOTA_A_ID" "$TOKEN_EMPLEADO"
esperar "Empleado ve la mascota de cualquier cliente" 200

peticion POST "/api/citas?mascotaId=$MASCOTA_A_ID&veterinarioId=$EMPLEADO_ID&fechaHora=2026-12-01T10:00:00&motivo=Control" "$TOKEN_A"
esperar "Cliente A agenda una cita para su mascota" 200
CITA_ID=$(campo_num id)

peticion POST "/api/citas?mascotaId=$MASCOTA_A_ID&veterinarioId=$EMPLEADO_ID&fechaHora=2026-12-02T10:00:00&motivo=Control" "$TOKEN_B"
esperar "Cliente B intenta agendar una cita para la mascota de A" 403

peticion GET "/api/citas/$CITA_ID" "$TOKEN_B"
esperar "Cliente B intenta ver la cita de A" 403

peticion DELETE "/api/citas/$CITA_ID" "$TOKEN_B"
esperar "Cliente B intenta cancelar la cita de A" 403

peticion GET "/api/expedientes/mascota/$MASCOTA_A_ID" "$TOKEN_B"
esperar "Cliente B intenta ver el historial clínico de la mascota de A" 403

peticion GET "/api/usuarios/$CLIENTE_A_ID" "$TOKEN_B"
esperar "Cliente B intenta ver el perfil de A" 403

peticion GET "/api/usuarios/$CLIENTE_A_ID" "$TOKEN_A"
esperar "Cliente A ve su propio perfil" 200

peticion POST "/api/citas?mascotaId=$MASCOTA_A_ID&veterinarioId=$EMPLEADO_ID&fechaHora=manana-10am&motivo=Control" "$TOKEN_A"
esperar "Fecha con formato inválido responde 400, no 500" 400

# =============================================================================
seccion "5. Inyección SQL"
# =============================================================================
peticion GET "/api/productos/categoria?categoria=%27%20OR%20%271%27%3D%271" "$TOKEN_A"
esperar "Categoría con ' OR '1'='1 (JPA usa consultas parametrizadas)" 200
if [ "$CUERPO" = "[]" ]; then pasa "La inyección no devolvió todos los productos (lista vacía)"; else falla "La inyección no devolvió datos" "respuesta: $(printf '%s' "$CUERPO" | head -c 160)"; fi

peticion GET "/api/productos/categoria?categoria=x%27%20UNION%20SELECT%20email%2Cpassword%20FROM%20usuarios--" "$TOKEN_A"
esperar "Categoría con UNION SELECT sobre la tabla usuarios" 200
no_contiene "No se filtraron emails ni contraseñas" "@petsalud.test"

login "' OR '1'='1" "cualquiera"
esperar "Login con ' OR '1'='1 como email (bloqueado por validación)" 400

login gerente@petsalud.test "' OR '1'='1"
esperar "Login con ' OR '1'='1 como contraseña" 401

# =============================================================================
seccion "6. XSS (Cross-Site Scripting)"
# =============================================================================
peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"<script>alert(1)</script>\",\"apellido\":\"Soto\",\"email\":\"xss1.$SUFIJO@petsalud.test\",\"password\":\"$PASS_CLIENTES\",\"direccion\":\"Av. Uno 123\"}"
esperar "Registro con <script> en el nombre" 400

peticion POST /api/usuarios/registro "" \
    "{\"nombre\":\"Ana\",\"apellido\":\"Soto\",\"email\":\"xss2.$SUFIJO@petsalud.test\",\"password\":\"$PASS_CLIENTES\",\"direccion\":\"<img src=x onerror=alert(1)>\"}"
esperar "Registro con <img onerror> en la dirección" 400

peticion GET "/api/productos/categoria?categoria=%3Cscript%3Ealert(1)%3C%2Fscript%3E" "$TOKEN_A"
esperar "Búsqueda con <script> en la URL (queda registrada en el monitoreo)" 200
cabecera "Respuesta en JSON, no HTML ejecutable" "^content-type: application/json"
cabecera "Cabecera X-Content-Type-Options: nosniff" "^x-content-type-options: nosniff"
cabecera "Cabecera X-Frame-Options: DENY (evita clickjacking)" "^x-frame-options: DENY"
cabecera "Cabecera Cache-Control: no-store (no guarda datos sensibles)" "^cache-control:.*no-store"

# =============================================================================
seccion "7. CSRF (Cross-Site Request Forgery)"
# =============================================================================
CODIGO=$(curl -s -o "$TMP/cuerpo" -w '%{http_code}' -X POST "$BASE_URL/api/productos" \
    -H "Origin: https://sitio-malicioso.example" \
    -H "Cookie: JSESSIONID=cookie-robada" \
    -H "Content-Type: application/json" \
    --data '{"nombre":"Producto falso"}')
CUERPO=$(cat "$TMP/cuerpo")
esperar "Petición desde otro sitio con cookie y sin token es rechazada" 401

# =============================================================================
seccion "8. Fuerza bruta (monitoreo)"
# =============================================================================
for intento in 1 2 3 4 5; do
    login gerente@petsalud.test "Incorrecta$intento"
done
esperar "5 intentos fallidos seguidos responden 401" 401
printf "  %sRevisa la consola de la aplicación: debe aparecer [SEGURIDAD][ALERTA] Posible ataque de fuerza bruta%s\n" "$GRIS" "$NC"

# =============================================================================
seccion "9. Limpieza de los datos de prueba"
# =============================================================================
peticion DELETE "/api/usuarios/$CLIENTE_B_ID" "$TOKEN_A"
esperar "Un cliente no puede eliminar a otro usuario" 403

peticion DELETE "/api/usuarios/$CLIENTE_A_ID" "$TOKEN_GERENTE"
esperar "Gerente elimina al cliente A (y sus mascotas y citas)" 204

peticion DELETE "/api/usuarios/$CLIENTE_B_ID" "$TOKEN_GERENTE"
esperar "Gerente elimina al cliente B" 204

# =============================================================================
printf "\n%s== Resultado ==%s\n" "$AZUL" "$NC"
printf "  %s%d pasaron%s · %s%d fallaron%s\n\n" "$VERDE" "$PASAN" "$NC" "$ROJO" "$FALLAN" "$NC"
[ "$FALLAN" -eq 0 ]
