# CLASSLA connector for Solr

Lucene analyzer that calls a gRPC service, where CLASSLA tokenizes, stems and lematizes the text.

Solr is a search platform that provides an indexing engine for documents. It uses Lucene, a natural language processing framework, which also provides sofisticated tokenization and stemming.

[CLASSLA](https://github.com/clarinsi/classla) (fork of [Stanza](https://github.com/stanfordnlp/stanza)) is a python library can process Slovenian, Croatian, Serbian, Macedonian and Bulgarian using PyTorch neural networks.

## Structure

This project provides two pieces of software:

- `lucene-grpc` is a java library, that provides a Lucene analyzer, which delagates input text to a gRPC service.
- `classla-grpc` is a Python microservice, that provides a gRPC service, which uses CLASSLA to process text.

Directory `proto` contains gRPC interface definition.

## How to use

1. Compile `lucene-grpc` to a JAR file, using Gradle's `jar` task,
2. Place the JAR file class path of Solr. Solr provides a `sharedLib` configuration option in `solr.xml`, which specifies a path from where .jar files will be loaded.
3. run `classla-grpc`. See [its README.md](./classla-grpc/README.md) for instructions on how to set up appropriate Python environment.
4. add analyzer to your Solr core's schema:
   ```xml
   <!-- Slovenian (for all fields that end with `_txt_sl`) -->
   <dynamicField name="*_txt_sl" type="text_sl" indexed="true" stored="true"/>
   <fieldType name="text_sl" class="solr.TextField">
       <analyzer class="eu.murerzen.lucene_grpc.GrpcAnalyzer"/>
   </fieldType>
   ```

### Docker

There is docker-compose prepared. It builds two images, but requires lucene-grpc to be built outside of docker.

    $ cd lucene-grpc
    $ ./gradlew jar
    $ cd ..
    $ docker-compose up

### Development

Build `lucene-grcp`:

    $ cd lucene-grcp
    $ ./gradlew jar

Clone and set-up Solr with a new core configuration:

    $ git clone https://github.com/apache/solr.git --depth=1
    $ cd solr
    $ ./gradlew assemble
    $ cp -r solr/server/solr/configsets/_default/conf/ solr/packaging/build/solr-10.0.0-SNAPSHOT/server/solr/my_core/

This will build Solr to `solr/packaging/build/solr-10.0.0-SNAPSHOT/`. 

Edit `my_core/managed-schema.xml` and add `<dynamicField>` and `<fieldType>` from above.

Edit `my_core/solr.xml` and change value of `<str name="sharedLib">` to `{location of this repo}/lucene-grpc/build/libs`.

Now you can start Solr with:

    $ cd solr/packaging/build/solr-10.0.0-SNAPSHOT/
    $ ./bin/solr start

Visit <http://localhost:8983> and add the new core you already configured.

Start `classla-grpc` (you will need [poetry](https://python-poetry.org/)):

    $ cd classla-grpc
    $ poetry install
    $ poetry run python src/main.py

This will run the gRPC server.

Now you can add and query documents in Solr web panel. Try adding following documents:

```json
{
  "id":"post-1",
  "naslov_txt_sl":"V Ljubljani je slabo vreme, saj pada veliko dežja",
  "naslov_txt_en": "Weather in Ljubljana is bad, since there is a lot of rain"
},
{
  "id":"post-2",
  "naslov_txt_sl":"Slovenija ima gospodarsko ureditev, ki temelji na prostem trgu",
  "naslov_txt_en": "Slovenia has an economic system based on the free market"
},
{
  "id":"post-3",
  "naslov_txt_sl":"Od treh mednarodnih letališč v Sloveniji je letališče Jožeta Pučnika Ljubljana v osrednji Sloveniji najbolj obremenjeno",
  "naslov_txt_en": "Of the three international airports in Slovenia, Ljubljana Jože Pučnik Airport in central Slovenia is the busiest"
},
{
  "id":"post-4",
  "naslov_txt_sl":"Po podatkih popisa prebivalstva leta 2002 je bilo med prebivalci Republike Slovenije 83,06 % Slovencev",
  "naslov_txt_en": "According to the 2002 census, 83.06% of the population of the Republic of Slovenia were Slovenes"
}
```

If you search for:
- `q=naslov_txt_sl:dež je padal` Solr should return `post-1`,
- `q=naslov_txt_sl:Ljubljane` Solr should return `post-1` and `post-3`,
- `q=naslov_txt_sl:bil` Solr should return `post-1`, `post-3` and `post-4`,

## Stability

Currently, this project is experimental. It provides all described functionality, 
but it does not have unit tests, has not been tested on large-scale deployments and
does not give any guarantee about stability or performance.