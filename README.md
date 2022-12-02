# BloomTest

This is a library to test Bloom filter indexing strategies.  It requires the as of yet unrelease Commons-Collections 4.5 that can be found at https://github.com/apache/commons-collections or .https://github.com/Claudenw/commons-collections

New tests can be added by extending the BloomIndex abstract class in the org.xenei.bloompaper.index package and then adding the class to the list of constructors in the static init() in the org.xenei.bloompaper.Test class.

## Data #

Data for executing the tests should be downloaded from http://download.geonames.org/export/dump/allCountries.zip and extracted into src/main/resources as allCountries.txt

Gatekeeper implementations create Bloom filters from the geonameid only.

Reference implementations create Bloom filters from the name, feature code, and country code.

## Applications ##
All applications use `-h` or `--help` as to display the help for the applications.

- org.xenei.bloompaper.Test -- Run the tests and optionally save the output data.
- org.xenei.bloompaper.Summary -- Create summary CSV files from saved `run` output.
- org.xenei.bloompaper.SplitSummary -- Splits the summary CSV file into `load`, `complete`, `name`, and `feature` specific summary files.
- org.xenei.bloompaper.Density -- Calculates the saturation of bloom filters as more filters are merged together.
