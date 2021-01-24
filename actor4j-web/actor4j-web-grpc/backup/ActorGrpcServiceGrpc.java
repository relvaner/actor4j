package actor4j.web.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.12.0)",
    comments = "Source: ActorGRPCService.proto")
public final class ActorGrpcServiceGrpc {

  private ActorGrpcServiceGrpc() {}

  public static final String SERVICE_NAME = "actor4j.web.grpc.ActorGRPCService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  @java.lang.Deprecated // Use {@link #getSendMethod()} instead. 
  public static final io.grpc.MethodDescriptor<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest,
      actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> METHOD_SEND = getSendMethodHelper();

  private static volatile io.grpc.MethodDescriptor<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest,
      actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> getSendMethod;

  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static io.grpc.MethodDescriptor<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest,
      actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> getSendMethod() {
    return getSendMethodHelper();
  }

  private static io.grpc.MethodDescriptor<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest,
      actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> getSendMethodHelper() {
    io.grpc.MethodDescriptor<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest, actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> getSendMethod;
    if ((getSendMethod = ActorGrpcServiceGrpc.getSendMethod) == null) {
      synchronized (ActorGrpcServiceGrpc.class) {
        if ((getSendMethod = ActorGrpcServiceGrpc.getSendMethod) == null) {
          ActorGrpcServiceGrpc.getSendMethod = getSendMethod = 
              io.grpc.MethodDescriptor.<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest, actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "actor4j.web.grpc.ActorGRPCService", "send"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new ActorGRPCServiceMethodDescriptorSupplier("send"))
                  .build();
          }
        }
     }
     return getSendMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ActorGRPCServiceStub newStub(io.grpc.Channel channel) {
    return new ActorGRPCServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ActorGRPCServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ActorGRPCServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ActorGRPCServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ActorGRPCServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class ActorGRPCServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void send(actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest request,
        io.grpc.stub.StreamObserver<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendMethodHelper(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendMethodHelper(),
            asyncUnaryCall(
              new MethodHandlers<
                actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest,
                actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse>(
                  this, METHODID_SEND)))
          .build();
    }
  }

  /**
   */
  public static final class ActorGRPCServiceStub extends io.grpc.stub.AbstractStub<ActorGRPCServiceStub> {
    private ActorGRPCServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ActorGRPCServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ActorGRPCServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ActorGRPCServiceStub(channel, callOptions);
    }

    /**
     */
    public void send(actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest request,
        io.grpc.stub.StreamObserver<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendMethodHelper(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ActorGRPCServiceBlockingStub extends io.grpc.stub.AbstractStub<ActorGRPCServiceBlockingStub> {
    private ActorGRPCServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ActorGRPCServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ActorGRPCServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ActorGRPCServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse send(actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest request) {
      return blockingUnaryCall(
          getChannel(), getSendMethodHelper(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ActorGRPCServiceFutureStub extends io.grpc.stub.AbstractStub<ActorGRPCServiceFutureStub> {
    private ActorGRPCServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ActorGRPCServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ActorGRPCServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ActorGRPCServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse> send(
        actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSendMethodHelper(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ActorGRPCServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ActorGRPCServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND:
          serviceImpl.send((actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCRequest) request,
              (io.grpc.stub.StreamObserver<actor4j.web.grpc.ActorGrpcServiceOuterClass.ActorGRPCResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ActorGRPCServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ActorGRPCServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return actor4j.web.grpc.ActorGrpcServiceOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ActorGRPCService");
    }
  }

  private static final class ActorGRPCServiceFileDescriptorSupplier
      extends ActorGRPCServiceBaseDescriptorSupplier {
    ActorGRPCServiceFileDescriptorSupplier() {}
  }

  private static final class ActorGRPCServiceMethodDescriptorSupplier
      extends ActorGRPCServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ActorGRPCServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ActorGrpcServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ActorGRPCServiceFileDescriptorSupplier())
              .addMethod(getSendMethodHelper())
              .build();
        }
      }
    }
    return result;
  }
}
