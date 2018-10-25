#!/bin/bash -x

wget -N http://www.unicode.org/Public/UNIDATA/{Blocks.txt,CJKRadicals.txt,Unihan.zip}
unzip -u Unihan.zip Unihan_RadicalStrokeCounts.txt Unihan_Readings.txt Unihan_Variants.txt Unihan_IRGSources.txt Unihan_OtherMappings.txt




