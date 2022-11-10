package main

import (
	"flag"
	"fmt"
	"log"
	"net"

	"github.com/envoy/examples/grpc-s2s/service"
	"golang.org/x/net/context"
	"google.golang.org/grpc"
	healthz "google.golang.org/grpc/health"
	healthsvc "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/reflection"
)

type helloService struct {
	service.UnimplementedHelloServer
}

func (s *helloService) Greet(
	ctx context.Context,
	in *service.HelloRequest,
) (*service.HelloResponse, error) {
	log.Println("Hello: Received request")
	// TODO call world service here
	return &service.HelloResponse{Reply: "hello"}, nil
}

func updateServiceHealth(
	h *healthz.Server,
	service string,
	status healthsvc.HealthCheckResponse_ServingStatus,
) {
	h.SetServingStatus(
		service,
		status,
	)
}

func main() {
	port := flag.Int("port", 8081, "grpc port")

	flag.Parse()

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	gs := grpc.NewServer()

	h := helloService{}
	service.RegisterHelloServer(gs, &h)
	reflection.Register(gs)

	healthServer := healthz.NewServer()
	healthsvc.RegisterHealthServer(gs, healthServer)
	updateServiceHealth(
		healthServer,
		service.Hello_ServiceDesc.ServiceName,
		healthsvc.HealthCheckResponse_SERVING,
	)

	log.Printf("starting grpc on :%d\n", *port)
	gs.Serve(lis)
}
