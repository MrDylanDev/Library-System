# Guía del usuario — LibroMágico

Guía para usar el catálogo, pedir préstamos, devolver libros y gestionar tus multas. Está
pensada para usuarios finales (rol **USUARIO**).

## Empezar

### Crear una cuenta

1. Entra en la portada y toca **Registrarse**.
2. Completa nombre, correo electrónico y contraseña.
3. Confirma el registro desde el correo que recibes (o entra directamente si el registro no
   requiere verificación).

### Iniciar sesión

Toca **Iniciar sesión**, escribe tu correo y contraseña y continúa. El acceso se recuerda en tu
navegador mientras no cierres sesión.

> **Entorno de desarrollo**: si el sistema se entrega con datos de prueba, puedes entrar con
> `usuario@libromagico.com` y contraseña `usuario123`. También existe un usuario `moroso` de
> ejemplo (`moroso@libromagico.com` / `moroso123`) con multas pendientes, útil para probar el
> pago de multas.

## El catálogo

### Buscar libros

En el catálogo puedes buscar por **título, autor, categoría o ISBN**. Escribe el texto en el
buscador y pulsa buscar. Los resultados se muestran con paginación: usa los controles para
pasar de página.

### Ver el detalle de un libro

Toca un libro para ver su detalle: autor, categoría, editorial, ISBN y **estado**.

Los estados posibles son:

| Estado | Significado |
|--------|-------------|
| **DISPONIBLE** | El libro está en la biblioteca y puedes pedirlo. |
| **PRESTADO** | Hay un ejemplar prestado; vuelve más tarde. |
| **PERDIDO** | El libro se dio por perdido y no se puede prestar. |

## Pedir un préstamo

1. Abre el detalle de un libro con estado **DISPONIBLE**.
2. Toca el botón **Prestar**.
3. El préstamo queda registrado en **Mis préstamos** por un período de **15 días**.

Solo puedes pedir un préstamo si has iniciado sesión. El libro se considera prestado hasta
que lo devuelvas.

## Devolver un libro

1. Entra en **Mis préstamos**.
2. En el préstamo activo, toca **Devolver**.

El botón **Devolver** solo aparece mientras el préstamo esté activo. Si pasaron los 15 días
sin devolverlo, el préstamo pasa a estado **atrasado** y se genera una multa (ver siguiente
sección).

## Multas

### Por qué se generan

Si devuelves un libro **después de los 15 días**, el sistema genera una multa de **$10** por
cada préstamo atrasado. Las multas pendientes se ven en **Mis multas**.

### Pagar una multa

1. Entra en **Mis multas**.
2. Toca **Pagar** en la multa que quieras saldar.

Al pagar, la multa queda saldada y deja de aparecer como pendiente.

## Recuperar la contraseña

1. En la pantalla de inicio de sesión, toca **¿Olvidaste tu contraseña?**.
2. Escribe tu correo.
3. Recibirás un correo con instrucciones para restablecerla. La contraseña nueva debe ser
   distinta de las anteriores.

## Preguntas frecuentes

**¿Cuánto tiempo puedo tener un libro prestado?**
15 días. Pasado ese plazo se genera una multa de $10.

**¿Qué pasa si el libro aparece como PRESTADO?**
Alguien más lo tiene prestado. Puedes revisarlo más tarde o buscar otro.

**¿Puedo pedir más de un libro a la vez?**
Sí, cada libro DISPONIBLE es un préstamo independiente. Ten en cuenta los plazos de cada uno.

**¿Cómo sé si tengo multas?**
En **Mis multas**, donde aparecen las pendientes con su monto y el botón **Pagar**.

**¿Puedo pagar una multa yo mismo?**
Sí. El pago de tus propias multas se hace desde **Mis multas**. (El personal de administración
también puede gestionarlas por ti.)

**¿Pierdo el acceso a la biblioteca por tener multas?**
Las multas no bloquean tu cuenta. Un bloqueo solo lo aplica un administrador (estado
**BLOQUEADO**), y eso impide iniciar sesión hasta que se reactive.