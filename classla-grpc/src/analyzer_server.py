from concurrent import futures
import logging

import grpc
import classla_pb2
import classla_pb2_grpc

logger = logging.getLogger()

class ClasslaAnalyzer(classla_pb2_grpc.AnalyzerServicer):
    def __init__(self, nlp):
        self.nlp = nlp

    def Analyze(self, request, context):
        logger.debug(f'received text "{request.text}"')

        tokens = []

        if len(request.text) > 0:

            doc = self.nlp(request.text)

            for t in doc.iter_tokens():
                for w in t.words:
                    tokens.append(classla_pb2.Token(lemma=w.lemma, upos=w.upos))

        return classla_pb2.AnalyzeReply(tokens=tokens)


def serve(analyzer):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    classla_pb2_grpc.add_AnalyzerServicer_to_server(analyzer, server)
    
    listen_address = "[::]:50051"
    server.add_insecure_port(listen_address)
    server.start()
    logger.info(f"listening for gRPC calls on {listen_address}")
    server.wait_for_termination()
