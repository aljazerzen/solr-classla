import logging

import grpc
import classla_pb2
import classla_pb2_grpc



def analyze(text):
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    with grpc.insecure_channel('localhost:50051') as channel:
        stub = classla_pb2_grpc.AnalyzerStub(channel)
        response = stub.Analyze(classla_pb2.AnalyzeRequest(text=text))
    
    print('\nResponse:')
    print('tokens:')
    for t in response.tokens:
        print(f'- {t.lemma} ({t.upos})')


if __name__ == '__main__':
    logging.basicConfig()
    
    print('Enter text to send to analyzer:')
    text = input()

    analyze(text)
