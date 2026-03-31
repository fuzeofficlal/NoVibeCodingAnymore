package proxy

import (
	"fmt"
	"net/http"
	"net/http/httputil"
	"net/url"

	"github.com/gin-gonic/gin"
)

func NewReverseProxy(target string) gin.HandlerFunc {
	targetURL, err := url.Parse(target)
	if err != nil {
		panic(fmt.Sprintf("invalid proxy target URL: %s", target))
	}

	rp := httputil.NewSingleHostReverseProxy(targetURL)

	rp.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadGateway)
		_, _ = w.Write([]byte(`{"status":502,"message":" Please ensure Spring Boot is running on port 8080."}`))
	}

	return func(c *gin.Context) {
		c.Request.URL.Path = c.Param("proxyPath")
		rp.ServeHTTP(c.Writer, c.Request)
	}
}
