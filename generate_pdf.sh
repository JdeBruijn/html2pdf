
rm -rf classes/

mkdir classes

jars="$(find -iname "*.jar" | tr '\n' ':' | sed 's/\.\///g')"

echo "jars=$jars"

javac -cp "$jars." -d classes/ src/*.java

if [ $? -gt 0 ]
then
	echo "Failed to compile"
	exit 1
fi

#output_format="$1";

#java -cp "$jars.:./classes/" $output_format $2 $3

java -cp "$jars.:./classes/" Main "pdf" $1 $2
