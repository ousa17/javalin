/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.misc.TypedException
import io.javalin.util.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestExceptionMapper {

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, http ->
        app.get("/unmapped-exception") { throw Exception() }
        assertThat(http.get("/unmapped-exception").status).isEqualTo(500)
        assertThat(http.getBody("/unmapped-exception")).isEqualTo("Internal server error")
    }

    @Test
    fun `mapped exceptions are handled`() = TestUtil.test { app, http ->
        app.get("/mapped-exception") { throw Exception() }
                .exception(Exception::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-exception").status).isEqualTo(200)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `HttpResponseException subclass handler is used`() = TestUtil.test { app, http ->
        app.get("/mapped-http-response-exception") { throw NotFoundResponse() }
                .exception(NotFoundResponse::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-http-response-exception").status).isEqualTo(200)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `HttpResponseException handler is used for subclasses`() = TestUtil.test { app, http ->
        app.get("/mapped-http-response-exception") { throw NotFoundResponse() }
                .exception(HttpResponseException::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-http-response-exception").status).isEqualTo(200)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `type information of exception is not lost`() = TestUtil.test { app, http ->
        app.get("/typed-exception") { throw TypedException() }
                .exception(TypedException::class.java) { e, ctx -> ctx.result(e.proofOfType()) }
        assertThat(http.get("/typed-exception").status).isEqualTo(200)
        assertThat(http.getBody("/typed-exception")).isEqualTo("I'm so typed")
    }

    @Test
    fun `most specific exception handler handles exception`() = TestUtil.test { app, http ->
        app.get("/exception-priority") { throw TypedException() }
                .exception(Exception::class.java) { _, ctx -> ctx.result("This shouldn't run") }
                .exception(TypedException::class.java) { _, ctx -> ctx.result("Typed!") }
        assertThat(http.get("/exception-priority").status).isEqualTo(200)
        assertThat(http.getBody("/exception-priority")).isEqualTo("Typed!")
    }

    @Test
    fun `catch-all Exception mapper doesn't override 404`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(500) }
        assertThat(http.get("/not-found").status).isEqualTo(404)
    }

    @Test
    fun `catch-all Exception mapper doesn't override HttpResponseExceptions`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(500) }
        app.get("/") { throw BadRequestResponse() }
        assertThat(http.get("/").status).isEqualTo(400)
    }

}
