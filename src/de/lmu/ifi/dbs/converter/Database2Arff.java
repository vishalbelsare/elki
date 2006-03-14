package de.lmu.ifi.dbs.converter;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class Database2Arff<D extends DatabaseObject&WekaObject>
{
    
    private static final String SEPARATOR = ",";
    
    public void convertToArff(Database<D> database, OutputStream stream)
    {
        PrintStream out = new PrintStream(stream);
        convertToArff(database,out);
        out.close();
    }
    
    public void convertToArff(Database<D> database, PrintStream out)
    {
        Map<Integer,Set<WekaAttribute>> nominalValues = new HashMap<Integer,Set<WekaAttribute>>();
        BitSet stringAttributes = new BitSet();
        final int NOT_INITIALIZED = -1;
        int dim = NOT_INITIALIZED;
        for(Iterator<Integer> dbIterator = database.iterator(); dbIterator.hasNext();)
        {
            Integer id = dbIterator.next();
            D databaseObject = database.get(id);
            WekaAttribute[] attributes = databaseObject.getAttributes();
            if(dim==NOT_INITIALIZED)
            {
                for(int i = 0; i < attributes.length; i++)
                {
                    if(attributes[i].isNominal())
                    {
                        nominalValues.put(i, new TreeSet<WekaAttribute>());
                    }
                    stringAttributes.set(i,attributes[i].isString());
                }
                dim = attributes.length;
            }
            else if(dim != attributes.length)
            {
                throw new IllegalArgumentException("DatabaseObject with id "+id+" differs in its dimensionality from previous objects.");
            }
            for(Integer i : nominalValues.keySet())
            {
                nominalValues.get(i).add(attributes[i]);
            }
        }
        out.print("@relation \"");
        out.print(new Date().toString());
        out.println("\"");
        out.println();
        for(int d = 0; d < dim; d++)
        {
            out.print("@attribute d");
            out.print(Integer.toString(d+1));
            out.print(" ");
            if(stringAttributes.get(d))
            {
                out.print(WekaAttribute.STRING);
            }
            else if(nominalValues.containsKey(d))
            {
                out.print("{");
                Util.print(new ArrayList<WekaAttribute>(nominalValues.get(d)),SEPARATOR,out);
                out.print("}");
            }
            else
            {
                out.print(WekaAttribute.NUMERIC);
            }
            out.println();
        }
        boolean printLabel = database.isSet(AssociationID.LABEL);
        if(printLabel)
        {
            out.print("@attribute label ");
            out.print(WekaAttribute.STRING);
            out.println();
        }
        boolean printClass = database.isSet(AssociationID.CLASS);
        if(printClass)
        {
            out.print("@attribute class ");
            out.print("{");
            Util.print(new ArrayList<ClassLabel>(Util.getClassLabels(database)),SEPARATOR,out);
            out.print("}");
            out.println();
        }
        out.println();
        out.println("@data");
        for(Iterator<Integer> dbIterator = database.iterator(); dbIterator.hasNext();)
        {
            Integer id = dbIterator.next();
            D d = database.get(id);
            WekaAttribute[] attributes = d.getAttributes();
            Util.print(Arrays.asList(attributes), SEPARATOR, out);
            if(printLabel)
            {
                out.print(SEPARATOR);
                out.print("\"");
                out.print(database.getAssociation(AssociationID.LABEL, id));
                out.print("\"");
            }
            if(printClass)
            {
                out.print(SEPARATOR);
                out.print(database.getAssociation(AssociationID.CLASS, id));
            }
            out.println();
        }
        out.flush();
    }
}
