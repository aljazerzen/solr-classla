import logging
import classla
import os

from analyzer_server import serve, ClasslaAnalyzer


def init_classla():
    dir = os.path.join('.', 'classla_resources')
    
    # download standard models for Slovenian
    # available: hr, sr, bg, mk
    classla.download("sl", processors="tokenize,pos,lemma", dir=dir)

    # initialize the default Slovenian pipeline
    nlp = classla.Pipeline("sl", processors="tokenize,pos,lemma", dir=dir)

    # run a test string trough the pipeline
    if False:
        test_string = "France Prešeren je bil rojen v Vrbi."
        doc = nlp(test_string)
        # print the output in CoNLL-U format
        # print(doc.to_conll())
        # print tokens only
        print(list(doc.iter_tokens()))

    return nlp


if __name__ == "__main__":
    nlp = init_classla()

    logging.basicConfig(level='INFO')

    analzyer = ClasslaAnalyzer(nlp)
    serve(analzyer)
