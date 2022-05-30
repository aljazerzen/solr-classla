# builds an image on top of solr:9

# requires you to first run `./gradlew jar` 

FROM solr:9 as solr

USER root

RUN sed -i "s/..solr[.]sharedLib:./\\/shared-lib/" /opt/solr/server/solr/solr.xml

# add the type into default schema
RUN sed -i $'s/<\/schema>/\n\
   <!-- Slovenian -->\n\
   <dynamicField name="*_txt_sl" type="text_sl" indexed="true" stored="true"\/>\n\
   <fieldType name="text_sl" class="solr.TextField">\n\
       <analyzer class="eu.murerzen.lucene_grpc.GrpcAnalyzer"\/>\n\
   <\/fieldType>\n\
<\/schema>/' /opt/solr/server/solr/configsets/_default/conf/managed-schema.xml

USER solr

COPY build/libs/lucene-grpc.jar /shared-lib/lucene-grpc.jar
