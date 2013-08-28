/*
 * Warning: This is a generated file. Edit at your own risk.
 * generated by Gen.hs on Wed Oct 28 18:26:49 CET 2009.
 */

package de.htwk.autolat.Connector.xmlrpc.parse;
import de.htwk.autolat.Connector.types.*;

import java.util.List;

@SuppressWarnings("unused")
public class InstanceParser
{
    private static final Parser<Instance> inst =
        new StructFieldParser<Instance>(
            "Instance",
            new Parser<Instance>()
            {
                Parser<String> tagParser = null;
                Parser<String> contentsParser = null;
                
                public Instance parse(Object val) throws ParseErrorBase
                {
                    if (tagParser == null)
                        tagParser = new StructFieldParser<String>(
                                        "tag",
                                        StringParser.getInstance());
                    if (contentsParser == null)
                        contentsParser = new StructFieldParser<String>(
                                             "contents",
                                             StringParser.getInstance());
                    return new Instance(
                        tagParser.parse(val),
                        contentsParser.parse(val));
                }
                
            }
            
        );
    
    public static Parser<Instance> getInstance()
    {
        return inst;
    }
    
}