package middleware

import (
	"net/http"
	"sync"

	"github.com/gin-gonic/gin"
	"golang.org/x/time/rate"
)

type clientLimiter struct {
	limiter *rate.Limiter
}

var (
	clients = make(map[string]*clientLimiter)
	mu      sync.Mutex
)

// 每个 IP 每秒最多 20 个请求，突发允许 40 个
func getLimiter(ip string) *rate.Limiter {
	mu.Lock()
	defer mu.Unlock()
	if cl, ok := clients[ip]; ok {
		return cl.limiter
	}
	l := rate.NewLimiter(20, 40)
	clients[ip] = &clientLimiter{limiter: l}
	return l
}

func RateLimit() gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()
		limiter := getLimiter(ip)
		if !limiter.Allow() {
			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
				"status":  429,
				"message": "Too many requests, please slow down.",
			})
			return
		}
		c.Next()
	}
}
