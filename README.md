# BloomTest

This is a library to test Bloom filter indexing strategies.  It requires the as of yet unrelease Commons-Collections that can be found at https://github.com/Claudenw/commons-collections

New test can be added by extending the BloomIndex abstract class in the org.xenei.bloompaper.index package and then adding the class to the list of constructors in the static init() in the org.xenei.bloompaper.Test class.

Data for executing the tests should be downloaded from http://download.geonames.org/export/dump/allCountries.zip and extracted into src/main/resources as allCountries.txt